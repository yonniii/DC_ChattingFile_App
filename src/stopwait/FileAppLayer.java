package stopwait;

import org.jnetpcap.Pcap;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.packet.PcapPacketHandler;

import javax.swing.*;
import java.io.*;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public class FileAppLayer implements BaseLayer{

    public class _FAPP_HEADER{
        byte[] fapp_totlen;
        byte[] fapp_type;
        byte fapp_msg_type;
        byte ed;
        byte[] fapp_seq_num;
        byte[] fapp_data;

        public _FAPP_HEADER(){
            this.fapp_totlen = new byte[4];
            this.fapp_type = new byte[2];
            this.fapp_msg_type = 0x00;
            this.ed = 0x00;
            this.fapp_seq_num = new byte[4];
            this.fapp_data = null;
        }
    }

    public boolean isReceiveACK_File = false;
    _FAPP_HEADER m_sHeader = new _FAPP_HEADER();
    // 11 12 13으로 타입


    class SendFile_Thread implements Runnable {
        byte[] data;
        Pcap AdapterObject;
        BaseLayer UpperLayer;

        public SendFile_Thread(Pcap m_AdapterObject, BaseLayer m_UpperLayer) {
            AdapterObject = m_AdapterObject;
            UpperLayer = m_UpperLayer;
        }

        @Override
        public void run() {
            while(true) {
                PcapPacketHandler<String> jpacketHandler = new PcapPacketHandler<String>() {
                    public void nextPacket(PcapPacket packet, String user) {
                        data = packet.getByteArray(0, packet.size());
                        UpperLayer.Receive(data);
                    }
                };

                AdapterObject.loop(100000,jpacketHandler,"");
            }
        }
    }


    public FileAppLayer(String pName) {
        // TODO Auto-generated constructor stub
        pLayerName = pName;
        ResetHeader();
    }
    public void ResetHeader(){
        m_sHeader = new _FAPP_HEADER();
    }

    byte[] intToByte2(int value) {
        byte[] temp = new byte[2];
        temp[0] = (byte) ((value & 0xFF00) >> 8);
        temp[1] = (byte) ((value) & 0xFF);

        return temp;
    }

    byte[] intToByte4(int value) {
        byte[] byteArray = new byte[4];
        byteArray[0] = (byte)(value >> 24);
        byteArray[1] = (byte)(value >> 16);
        byteArray[2] = (byte)(value >> 8);
        byteArray[3] = (byte)(value);
        return byteArray;
    }

    private byte[] getFileName(String filename){
        String[] name = filename.split("\\\\");
        return name[name.length-1].getBytes();
    }

    private byte[] ObjToByte(byte type,byte[] seqNum, byte[] input,int size){
        byte[] buf = new byte[size+12];
        byte[] typeArr = {type,type};
        m_sHeader.fapp_type = typeArr;
        m_sHeader.fapp_seq_num = seqNum;
        System.arraycopy(m_sHeader.fapp_totlen,0,buf,0,4);
        System.arraycopy(m_sHeader.fapp_type,0,buf,4,2);
        System.arraycopy(m_sHeader.fapp_seq_num,0,buf,8,4);
        for (int i = 0; i < size; i++) {
            buf[i+12] = input[i];
        }
        return buf;
    }

    private boolean WaitAck(){
        while (!isReceiveACK_File) {
            try {
                Thread.sleep(800);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        isReceiveACK_File = false;
        return true;
    }

    public boolean Send(String filename){
//        SendFile_Thread thread = new SendFile_Thread()

        EthernetLayer ethernetLayer = (EthernetLayer) this.GetUnderLayer();
        StopWaitDlg dlg = (StopWaitDlg) this.GetUpperLayer(0);
        System.out.println("FileApp - "+filename);
        byte[] buffer =  OpenFile(filename);
        byte[] totlen = intToByte4(buffer.length);
        byte[] name = getFileName(filename);
        byte[] dataToSend;
        m_sHeader.fapp_totlen=totlen;

        System.out.println(new String(buffer));
        dlg.progressBar.setMaximum(buffer.length);
        dlg.progressBar.setValue(0);

        dataToSend = ObjToByte((byte)0x10,intToByte4(0),name,name.length); //첫번째 이름과 크기와 타입 보냄
        ethernetLayer.SendFile(dataToSend, dataToSend.length);
//        System.out.println(new String(dataToSend));
        System.out.println("FileApp - Send 1 filename");

        int length = buffer.length;
        int sendIndex = buffer.length;
        byte[] frag= new byte[1448];
        while (sendIndex>1448){
            System.arraycopy(buffer,length-sendIndex,frag,0,1448);
            dataToSend = ObjToByte((byte)0x11,intToByte4(length-sendIndex),frag,1448);
            ethernetLayer.SendFile(dataToSend,1460);
            System.out.println("FileApp - Send 2 midledata");
            int prog = length-sendIndex;
//            dlg.updataBar(length-sendIndex);
            SwingUtilities.invokeLater(() -> dlg.progressBar.setValue(prog));
            sendIndex -=1448;
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
        }
        frag = new byte[sendIndex];
        System.arraycopy(buffer,length-sendIndex,frag,0,sendIndex);
        dataToSend = ObjToByte((byte)0x12,intToByte4(length-sendIndex),frag,sendIndex);
        ethernetLayer.SendFile(dataToSend,sendIndex+12);
        System.out.println("FileApp - Send 3 lastdata");
        dlg.progressBar.setValue(buffer.length);
//        dlg.progressBar.
        ResetHeader();

        return true;
    }

    private byte[] OpenFile(String filePath){
        BufferedInputStream bs = null;
        byte[] b=null;
        try
        {
            bs = new BufferedInputStream(new FileInputStream(filePath));
            b = new byte [bs.available()]; //임시로 읽는데 쓰는 공간
            System.out.println(b.length);
            while( bs.read(b) != -1) {}

            System.out.println(new String(b)); //필요에 따라 스트링객체로 변환

        }
        catch (Exception e)
        {
            System.out.println(e);
        }
        finally
        {
            try
            {
                bs.close(); //반드시 닫는다.
            }
            catch (Exception e)
            {
                System.out.println(e);
            }
        }
        return b;
    }

    ByteBuffer inputBuffer;
    String temp_filename;

    public boolean Receive(byte[] input) {
        if(input.length < 12)
            return false;
//        byte[] type = {input[4],input[5]};
        int totlen = byteToint4(input[0],input[1],input[2],input[3]);
        if(input[4]==0x10 && input[5]==0x10){
            System.out.println("FileApp - Receive 0x10");
            inputBuffer = ByteBuffer.allocate(totlen);
            temp_filename = new String( RemoveCappHeader(input,input.length));
        }else if(input[4]==0x11 &&input[5]==0x11){
            System.out.println("FileApp - Receive 0x11");
            inputBuffer.put(RemoveCappHeader(input,1460));
        }else if(input[4]==0x12&&input[5]==0x12){
            System.out.println("FileApp - Receive 0x12");
            inputBuffer.put(RemoveCappHeader(input,totlen%1448+12));
            String msg = temp_filename+"을 받았습니다.";
            OutputFile(inputBuffer.array());
            this.GetUpperLayer(0).Receive(msg.getBytes());
//            this.GetUpperLayer(0).Receive(inputBuffer.array());
            inputBuffer = ByteBuffer.allocate(0);
        }

        return false;
    }

    private void OutputFile(byte[] input){
        BufferedOutputStream bs = null;

        try
        {
            bs = new BufferedOutputStream(new FileOutputStream(temp_filename));
            bs.write(input,0,input.length); //Byte형으로만 넣을 수 있음
        }
        catch (Exception e)
        {
            System.out.println(e);
        }
        finally
        {
            try
            {
                bs.close(); //반드시 닫는다.
            }
            catch (Exception e)
            {
                System.out.println(e);
            }
        }
    }

    private byte[] RemoveCappHeader(byte[] input, int length) {//receive할 때 사용 / 데이터에 붙어있는 4바이트의 헤더를 제거
        byte[] buf = new byte[length - 12];//input보다 4만큼 작은 배열 선언
        for (int i = 0; i < length - 12; i++) {
            buf[i] = input[i + 12];//배열에 헤더 이후의 데이터를 옮김
        }
        return buf;
    }

    private int byteToint4(byte a, byte b, byte c, byte d) {
        return (a & 0xff)<<24 | (b & 0xff)<<16 | (c & 0xff)<<8 | (d & 0xff);
    }

    public int nUpperLayerCount = 0;
    public String pLayerName = null;
    public BaseLayer p_UnderLayer = null;
    public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();

    @Override
    public String GetLayerName() {
        // TODO Auto-generated method stub
        return pLayerName;
    }

    @Override
    public BaseLayer GetUnderLayer() {
        // TODO Auto-generated method stub
        if (p_UnderLayer == null)
            return null;
        return p_UnderLayer;
    }

    @Override
    public BaseLayer GetUpperLayer(int nindex) {
        // TODO Auto-generated method stub
        if (nindex < 0 || nindex > nUpperLayerCount || nUpperLayerCount < 0)
            return null;
        return p_aUpperLayer.get(nindex);
    }

    @Override
    public void SetUnderLayer(BaseLayer pUnderLayer) {
        // TODO Auto-generated method stub
        if (pUnderLayer == null)
            return;
        p_UnderLayer = pUnderLayer;
    }

    @Override
    public void SetUpperLayer(BaseLayer pUpperLayer) {
        // TODO Auto-generated method stub
        if (pUpperLayer == null)
            return;
        this.p_aUpperLayer.add(nUpperLayerCount++, pUpperLayer);
    }

    @Override
    public void SetUpperUnderLayer(BaseLayer pUULayer) {
        this.SetUpperLayer(pUULayer);
        pUULayer.SetUnderLayer(this);
    }
}
