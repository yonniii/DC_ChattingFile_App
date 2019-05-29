package chat_file;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class ChatAppLayer implements BaseLayer {

    public int nUpperLayerCount = 0;
    public String pLayerName = null;
    public BaseLayer p_UnderLayer = null;
    public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
    ByteBuffer buffer;
    public boolean isReceiveACK_Chat = false;


    private class _CHAT_APP {
        byte[] capp_totlen;
        byte capp_type;
        byte capp_unused;
        byte[] capp_data;

        public _CHAT_APP() {
            this.capp_totlen = new byte[2];
            this.capp_type = 0x00;
            this.capp_unused = 0x00;
            this.capp_data = null;
        }
    }

    _CHAT_APP m_sHeader = new _CHAT_APP();

    public ChatAppLayer(String pName) {
        // TODO Auto-generated constructor stub
        pLayerName = pName;
        ResetHeader();
    }

    public void ResetHeader() {
        m_sHeader = new _CHAT_APP();
    }


    public byte[] ObjToByte(byte[] data, int dataIndex, int length, byte[] temp_totlen, byte type) {
        byte[] buf = new byte[MAX_DATA_SIZE+HEADER_SIZE];
        m_sHeader.capp_totlen = temp_totlen;
        m_sHeader.capp_type = type;
        buf[0] = m_sHeader.capp_totlen[0];
        buf[1] = m_sHeader.capp_totlen[1];
        buf[2] = m_sHeader.capp_type;
        buf[3] = m_sHeader.capp_unused;
        for (int i = 0; i < length; i++) {
            buf[i + 4] = data[i + dataIndex];
        }
        return buf;
    }

    final int MAX_DATA_SIZE = 1456;
    final int HEADER_SIZE = 4;


    public boolean Send(byte[] input, int length) {
        byte[] buf;
        byte[] totlen = intToByte2(length);
        EthernetLayer ethernet = (EthernetLayer) this.GetUnderLayer();

        if (length > MAX_DATA_SIZE) {
            int sendIndex = length;

            buf = ObjToByte(input, 0, MAX_DATA_SIZE, totlen, (byte) 0x01);
            this.GetUnderLayer().Send(buf, MAX_DATA_SIZE+HEADER_SIZE);
            System.out.println("ChatApp - Send 0x01");
            sendIndex -= MAX_DATA_SIZE;

//            while (!isReceiveACK_Chat) {
//                try {
//                    Thread.sleep(800);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//            isReceiveACK_Chat = false;

            while (true) {
                if (sendIndex < MAX_DATA_SIZE+1)
                    break;
                buf = ObjToByte(input, length - sendIndex, MAX_DATA_SIZE, totlen, (byte) 0x02);
                this.GetUnderLayer().Send(buf, MAX_DATA_SIZE+HEADER_SIZE);
                System.out.println("ChatApp - Send 0x02");
                sendIndex -= MAX_DATA_SIZE;
//                while (!isReceiveACK_Chat) {
//                    try {
//                        Thread.sleep(800);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//                isReceiveACK_Chat = false;
            }

            buf = ObjToByte(input, length - sendIndex, sendIndex, totlen, (byte) 0x03);
            this.GetUnderLayer().Send(buf, buf.length);
            System.out.println("ChatApp - Send 0x03");
//            while (!isReceiveACK_Chat) {
//                try {
//                    Thread.sleep(800);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//            isReceiveACK_Chat = false;
            return true;

        } else {

            buf = ObjToByte(input, 0, length, totlen, (byte) 0x00);
            this.GetUnderLayer().Send(buf, MAX_DATA_SIZE+HEADER_SIZE);
//            while (!isReceiveACK_Chat) {
//                try {
//                    Thread.sleep(800);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//            isReceiveACK_Chat = false;
            return true;

        }
    }

    public byte[] RemoveCappHeader(byte[] input, int length) {//receive할 때 사용 / 데이터에 붙어있는 4바이트의 헤더를 제거
        byte[] buf = new byte[length - HEADER_SIZE];//input보다 4만큼 작은 배열 선언
        for (int i = 0; i < length - HEADER_SIZE; i++) {
            buf[i] = input[i + HEADER_SIZE];//배열에 헤더 이후의 데이터를 옮김
        }
        return buf;
    }

    public synchronized boolean Receive(byte[] input) {
        if (input.length < HEADER_SIZE+1)
            return false;
        byte type = input[2];
        int totlen = byteToint(input[0], input[1]);
        if (type != (byte) 0x00 && type != (byte) 0x01 && type != (byte) 0x02 && type != (byte) 0x03)
            return false;
        if (totlen > MAX_DATA_SIZE) {
            if (input[2] == (byte) 0x01) {
                System.out.println("ChatApp - Receive 0x01");
                buffer = ByteBuffer.allocate(totlen);
                System.out.println("after clear 0x01" + buffer.toString());
                buffer.put(RemoveCappHeader(input, MAX_DATA_SIZE+HEADER_SIZE));
                return true;
            } else if (input[2] == (byte) 0x02) {
                System.out.println("ChatApp - Receive 0x02");
                buffer.put(RemoveCappHeader(input, MAX_DATA_SIZE+HEADER_SIZE));
                return true;
            } else if (input[2] == (byte) 0x03) {
                System.out.println("ChatApp - Receive 0x03");
                System.out.println(((totlen/10)+1)*10 + " / " + totlen +" / "+(totlen%10+4));
                buffer.put(RemoveCappHeader(input, totlen%MAX_DATA_SIZE+HEADER_SIZE));
                this.GetUpperLayer(0).Receive(buffer.array());
//                buffer = ByteBuffer.allocate(totlen);
                System.out.println("after clear 0x03" + buffer.toString());
                return true;
            }
        } else {
            if (input[2] == (byte) 0x00) {
                buffer = ByteBuffer.allocate(totlen);
                System.out.println("ChatApp - Receive 0x00");
                this.GetUpperLayer(0).Receive(RemoveCappHeader(input, input.length));
                buffer = ByteBuffer.allocate(totlen);
                return true;
            }
        }

        return true;
    }

    byte[] intToByte2(int value) {
        byte[] temp = new byte[2];
        temp[0] = (byte) ((value & 0xFF00) >> 8);
        temp[1] = (byte) ((value) & 0xFF);

        return temp;
    }

    public int byteToint(byte i, byte j) {
        return (i & 0xff) << 8 | (j & 0xff);
    }


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
