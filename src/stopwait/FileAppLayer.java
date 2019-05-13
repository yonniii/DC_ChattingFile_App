package stopwait;

import org.jnetpcap.Pcap;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.packet.PcapPacketHandler;

import javax.swing.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public class FileAppLayer implements BaseLayer {

    public int getFileStatus() {
        return (int)file_status;
    }

    public void setFileStatus(int i) {
        this.file_status = i;
    }

    public class _FAPP_HEADER {
        byte[] fapp_totlen;
        byte[] fapp_type;
        byte fapp_msg_type;
        byte ed;
        byte[] fapp_seq_num;
        byte[] fapp_data;

        public _FAPP_HEADER() {
            this.fapp_totlen = new byte[4];
            this.fapp_type = new byte[2];
            this.fapp_msg_type = 0x00;
            this.ed = 0x00;
            this.fapp_seq_num = new byte[4];
            this.fapp_data = null;
        }
    }

    //    public boolean isReceiveACK_File = false;
    _FAPP_HEADER m_sHeader = new _FAPP_HEADER();

    // 11 12 13으로 타입


//    class SendFile_Thread implements Runnable {
//        byte[] data;
//        Pcap AdapterObject;
//        BaseLayer UpperLayer;
//
//        public SendFile_Thread(Pcap m_AdapterObject, BaseLayer m_UpperLayer) {
//            AdapterObject = m_AdapterObject;
//            UpperLayer = m_UpperLayer;
//        }
//
//        @Override
//        public void run() {
//            while (true) {
//                PcapPacketHandler<String> jpacketHandler = new PcapPacketHandler<String>() {
//                    public void nextPacket(PcapPacket packet, String user) {
//                        data = packet.getByteArray(0, packet.size());
//                        UpperLayer.Receive(data);
//                    }
//                };
//
//                AdapterObject.loop(100000, jpacketHandler, "");
//            }
//        }
//    } //안쓸듯??


    public FileAppLayer(String pName) {
        // TODO Auto-generated constructor stub
        pLayerName = pName;
        ResetHeader();
    }

    public void ResetHeader() {
        m_sHeader = new _FAPP_HEADER();
    }

    byte[] intToByte2(int value) {
        byte[] temp = new byte[2];
        temp[0] = (byte) ((value & 0xFF00) >> 8);
        temp[1] = (byte) ((value) & 0xFF);

        return temp;
    }

    byte[] intToByte4(int value) {
        byte[] temp = new byte[4];

        temp[0] |= (byte) ((value & 0xFF000000) >> 24);
        temp[1] |= (byte) ((value & 0xFF0000) >> 16);
        temp[2] |= (byte) ((value & 0xFF00) >> 8);
        temp[3] |= (byte) (value & 0xFF);
        return temp;
    }

    private int byteToint4(byte a, byte b, byte c, byte d) {
        int s1 = a & 0xFF;
        int s2 = b & 0xFF;
        int s3 = c & 0xFF;
        int s4 = d & 0xFF;

        return ((s1 << 24) + (s2 << 16) + (s3 << 8) + (s4 << 0));
    }

    private byte[] getFileName(String filename) {
        String[] name = filename.split("\\\\");
        return name[name.length - 1].getBytes();
    }

    private byte[] ObjToByte(byte[] type, byte[] seqNum, byte[] input, int size) {
        byte[] buf = new byte[size + 12];
        m_sHeader.fapp_type = type;
        m_sHeader.fapp_seq_num = seqNum;
        System.arraycopy(m_sHeader.fapp_totlen, 0, buf, 0, 4);
        System.arraycopy(m_sHeader.fapp_type, 0, buf, 4, 2);
        System.arraycopy(m_sHeader.fapp_seq_num, 0, buf, 8, 4);
        for (int i = 0; i < size; i++) {
            buf[i + 12] = input[i];
        }
        return buf;
    }

    //    private boolean WaitAck(){
//        while (!isReceiveACK_File) {
//            try {
//                Thread.sleep(800);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//        isReceiveACK_File = false;
//        return true;
//    }
    private byte[] OpenFile(String filePath) {
        BufferedInputStream bs = null;
        byte[] b = null;
        try {
            bs = new BufferedInputStream(new FileInputStream(filePath));
            b = new byte[bs.available()]; //임시로 읽는데 쓰는 공간
            System.out.println(b.length);
            while (bs.read(b) != -1) {
            }
//            System.out.println(new String(b)); //필요에 따라 스트링객체로 변환

        } catch (Exception e) {
            System.out.println("in FileApp OpenFile" + e);
        } finally {
            try {
                bs.close(); //반드시 닫는다.
            } catch (Exception e) {
                System.out.println("in FileApp OpenFile" + e);
            }
        }
        return b;
    }


    float file_status = 0;
    private final int MAX_DATA_SIZE = 1448;
    final int HEADER_SIZE = 12;
    final byte[] PACKET_TYPE_FIRST = {(byte) 0x10, (byte) 0x10};
    final byte[] PACKET_TYPE_MID = {(byte) 0x11, (byte) 0x11};
    final byte[] PACKET_TYPE_LAST = {(byte) 0x12, (byte) 0x12};

    public boolean Send(String filepath) {
        EthernetLayer ethernet = (EthernetLayer) this.GetUnderLayer();
        StopWaitDlg dlg = (StopWaitDlg) this.GetUpperLayer(0);
        System.out.println("FileApp - filepath : " + filepath);

        byte[] byte_file_Data = OpenFile(filepath); //경로에 저장된 파일 불러와서 바이트배열에 저장
        byte[] byte_file_name = getFileName(filepath); //파일의 이름 저장
        int int_Data_totlen = byte_file_Data.length; //파일의 총 길이 int형으로 저장
        byte[] byte_Data_totlen = intToByte4(int_Data_totlen); //파일의 총 길이 byte배열로 저장
        byte[] byte_buffer_ToSend; //파일에 대한 바이트배열 자르는 데에 사용할 임시 배열
        byte[] byte_buffer_data_withHEADER; //자른 배열에 헤더를 붙이고, underLayer에 보낼 배열
        m_sHeader.fapp_totlen = byte_Data_totlen; //헤더에 길이정보 업데이트

        byte_buffer_data_withHEADER = ObjToByte(PACKET_TYPE_FIRST, intToByte4(0), byte_file_name, byte_file_name.length); //첫번째 파일의 이름과 크기와 타입(0x10)을 헤더로 붙임
        System.out.println("FileApp - Send 1 filename");
        ethernet.SendFile(byte_buffer_data_withHEADER, byte_buffer_data_withHEADER.length); //첫번째 패킷 전송
//        int length = byte_file_Data.length;
//        int sendIndex = byte_file_Data.length;
//        byte[] frag = new byte[1448];
//        while (sendIndex > 1448) {
//            System.arraycopy(byte_file_Data, length - sendIndex, frag, 0, 1448);
//            byte_buffer_ToSend = ObjToByte((byte) 0x11, intToByte4(length - sendIndex), frag, 1448);
////            file_status = ((float) (i + 1) / (float) packet_count) * 100;
//            ethernet.SendFile(byte_buffer_ToSend, 1460);
//            System.out.println("FileApp - Send 2 midledata");
//            int prog = length - sendIndex;
//            SwingUtilities.invokeLater(() -> dlg.progressBar.setValue(prog));
//            sendIndex -= 1448;
//        }
//        frag = new byte[sendIndex];
//        System.arraycopy(byte_file_Data, length - sendIndex, frag, 0, sendIndex);
//        byte_buffer_ToSend = ObjToByte((byte) 0x12, intToByte4(length - sendIndex), frag, sendIndex);
//        ethernet.SendFile(byte_buffer_ToSend, sendIndex + 12);
//        System.out.println("FileApp - Send 3 lastdata");
//        dlg.progressBar.setValue(byte_file_Data.length);
        int int_seq_num; //중간 패킷의 수
        int int_last_packet_size; //마지막 패킷의 사이즈
        if ((int_last_packet_size = int_Data_totlen % MAX_DATA_SIZE) == 0) {
            int_seq_num = int_Data_totlen / MAX_DATA_SIZE - 1;
            int_last_packet_size = MAX_DATA_SIZE;
        } else {
            int_seq_num = int_Data_totlen / MAX_DATA_SIZE;
        }
        byte_buffer_ToSend = new byte[MAX_DATA_SIZE];

        for (int i = 0; i <= int_seq_num; i++) {
            try {
                Thread.sleep((long)1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (i != int_seq_num) { //중간패킷일 때
                System.arraycopy(byte_file_Data, MAX_DATA_SIZE * i, byte_buffer_ToSend, 0, MAX_DATA_SIZE);
                byte_buffer_data_withHEADER = ObjToByte(PACKET_TYPE_MID, intToByte4(i), byte_buffer_ToSend, MAX_DATA_SIZE);
                System.out.println("FileApp - Send 2 midledata" + i + "번 패킷");
                ethernet.SendFile(byte_buffer_data_withHEADER, MAX_DATA_SIZE + HEADER_SIZE);
            } else { //마지막 패킷일 때
                System.arraycopy(byte_file_Data, MAX_DATA_SIZE * i, byte_buffer_ToSend, 0, int_last_packet_size);
                byte_buffer_data_withHEADER = ObjToByte(PACKET_TYPE_LAST, intToByte4(i), byte_buffer_ToSend, int_last_packet_size);
                System.out.println("FileApp - Send 3 lastdata" + i + "번 패킷 마지막~~~~~~");
                ethernet.SendFile(byte_buffer_data_withHEADER, int_last_packet_size + HEADER_SIZE);
            }
            file_status = ((float) (i + 1) / (float) int_seq_num) * 100;
        }
        ResetHeader();
        return true;
    }


    ByteBuffer inputBuffer;
    String temp_filename;
    byte[] receive_data_buffer;
    int receive_packet_num = 0;

    private void OutputFile(byte[] input) {
        BufferedOutputStream bs = null;

        try {
            bs = new BufferedOutputStream(new FileOutputStream(temp_filename));
            bs.write(input, 0, input.length); //Byte형으로만 넣을 수 있음
        } catch (Exception e) {
            System.out.println("in FileApp OutputFile 에서...."+e);
        } finally {
            try {
                bs.close(); //반드시 닫는다.
            } catch (Exception e) {
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

    public int Received_packet_count=0;

//    public boolean Receive(byte[] input) {
//        if (input.length < 12)
//            return false;
////        byte[] type = {input[4],input[5]};
//        int totlen = byteToint4(input[0], input[1], input[2], input[3]);
//        if (input[4] == 0x10 && input[5] == 0x10) {
//            System.out.println("FileApp - Receive 0x10");
//            inputBuffer = ByteBuffer.allocate(totlen);
//            temp_filename = new String(RemoveCappHeader(input, input.length));
//        } else if (input[4] == 0x11 && input[5] == 0x11) {
//            System.out.println("FileApp - Receive 0x11");
//            inputBuffer.put(RemoveCappHeader(input, 1460));
//            Received_packet_count++;
//        } else if (input[4] == 0x12 && input[5] == 0x12) {
//            System.out.println("FileApp - Receive 0x12");
//            inputBuffer.put(RemoveCappHeader(input, totlen % 1448 + 12));
//            Received_packet_count++;
//            String msg = temp_filename + "을 받았습니다.";
//            OutputFile(inputBuffer.array());
//            this.GetUpperLayer(0).Receive(msg.getBytes());
////            this.GetUpperLayer(0).Receive(inputBuffer.array());
//            inputBuffer = ByteBuffer.allocate(0);
//        }
//
//        return false;
//    }


    public boolean Receive(byte[] input) {
        if (input.length < 12)
            return false;
        int int_Data_totlen = byteToint4(input[0], input[1], input[2], input[3]);
        byte[] byte_Packet_type = {input[4], input[5]};
        int int_last_packet_size; //마지막 패킷의 사이즈
        int int_seq_num;
        int_seq_num = byteToint4(input[8],input[9],input[10],input[11]);

        if ((int_last_packet_size = int_Data_totlen % MAX_DATA_SIZE) == 0) {
            int_last_packet_size = MAX_DATA_SIZE;
        }

        if (Arrays.equals(byte_Packet_type, PACKET_TYPE_FIRST)) { //첫번째 패킷일 때
            System.out.println("FileApp - Receive 0x10");
            receive_data_buffer = new byte[int_Data_totlen];
            temp_filename = new String(RemoveCappHeader(input, input.length)); //데이터에서 파일이름만 추출하여 필드에 저장
        } else if (Arrays.equals(byte_Packet_type, PACKET_TYPE_MID)) { //중간 패킷일 때
            System.out.println("FileApp - Receive 0x11" + int_seq_num+"번째 패킷");
            System.arraycopy(input, HEADER_SIZE, receive_data_buffer, int_seq_num * MAX_DATA_SIZE, MAX_DATA_SIZE);
            Received_packet_count++;
        } else if (Arrays.equals(byte_Packet_type, PACKET_TYPE_LAST)) { //마지막 패킷일 때
            System.out.println("FileApp - Receive 0x12");
            System.arraycopy(input, HEADER_SIZE, receive_data_buffer, int_seq_num * MAX_DATA_SIZE, int_last_packet_size);
            OutputFile(receive_data_buffer);
            Received_packet_count++;
            String msg = temp_filename + "을 받았습니다.";
            this.GetUpperLayer(0).Receive(msg.getBytes());
            receive_data_buffer=null;
        }

        int packet_count;
        if (int_Data_totlen % MAX_DATA_SIZE == 0)
            packet_count = int_Data_totlen / MAX_DATA_SIZE - 1;
        else
            packet_count = int_Data_totlen / MAX_DATA_SIZE;

        ((StopWaitDlg) GetUpperLayer(0)).set_progressBar(packet_count, Received_packet_count);


        return true;
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
