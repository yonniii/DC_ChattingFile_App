package chat_file;

import java.util.ArrayList;

public class EthernetLayer implements BaseLayer {

    public int nUpperLayerCount = 0;
    public String pLayerName = null;
    public BaseLayer p_UnderLayer = null;
    public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
    private boolean isReceiveACK = false;


    private class _ETHERNET_ADDR {
        private byte[] addr = new byte[6];

        public _ETHERNET_ADDR() {
            this.addr[0] = (byte) 0x00;
            this.addr[1] = (byte) 0x00;
            this.addr[2] = (byte) 0x00;
            this.addr[3] = (byte) 0x00;
            this.addr[4] = (byte) 0x00;
            this.addr[5] = (byte) 0x00;
        }
    }

    private class _ETHERNET_Frame {
        _ETHERNET_ADDR enet_dstaddr;
        _ETHERNET_ADDR enet_srcaddr;
        byte[] enet_type;
        byte[] enet_data;

        public _ETHERNET_Frame() {
            this.enet_dstaddr = new _ETHERNET_ADDR();
            this.enet_srcaddr = new _ETHERNET_ADDR();
            this.enet_type = new byte[2];
            this.enet_data = null;
        }
    }


    public int byteToint(byte i, byte j) {
        return (i & 0xff) << 8 | (j & 0xff);
    }

    byte[] intToByte2(int value) {
        byte[] temp = new byte[2];
        temp[0] = (byte) ((value & 0xFF00) >> 8);
        temp[1] = (byte) ((value) & 0xFF);

        return temp;
    }

    public void setDstAddress(byte[] address) {
        m_sHeader.enet_dstaddr.addr = address;
    }

    // dst주소를 설정하는 함수
    public void setSrcAddress(byte[] address) {
        m_sHeader.enet_srcaddr.addr = address;
    }

    // src주소를 설정하는 함수
    _ETHERNET_Frame m_sHeader = new _ETHERNET_Frame();

    public EthernetLayer(String pName) {
        // TODO Auto-generated constructor stub
        pLayerName = pName;
        ResetHeader();
    }

    public void ResetHeader() { //헤더에 들어갈 값을 00으로 초기화
        m_sHeader = new _ETHERNET_Frame();
        m_sHeader.enet_type[0] = (byte) 0x08;
    }
    final int Ether_HEADER_SIZE = 14;

    public byte[] ObjToByte(_ETHERNET_Frame Header, int length) {
        byte[] buf = new byte[length + Ether_HEADER_SIZE]; //input보다 14 큰 배열 선언
        System.arraycopy(Header.enet_dstaddr.addr, 0, buf, 0, 6); //배열의 0~5인덱스에 dst주소 붙임
        System.arraycopy(Header.enet_srcaddr.addr, 0, buf, 6, 6);//배열의 6~11인덱스에 src주소 붙임
        buf[12] = Header.enet_type[0];
        buf[13] = Header.enet_type[1];
        for (int i = 0; i < length; i++) {
            buf[i + Ether_HEADER_SIZE] = Header.enet_data[i];
        }
        return buf;
    }

    private boolean SendChat(byte[] input, int length){
        m_sHeader.enet_type[0] = (byte) 0x20; //chat이라고 타입(2080) 지정
        m_sHeader.enet_type[1] = (byte) 0x80;
        m_sHeader.enet_data = input;
        byte[] buf = ObjToByte(m_sHeader, length);//헤더를 붙이는 함수 호출
        System.out.println("Ethernet - ChatSend " + new String(buf));
        this.GetUnderLayer().Send(buf, length + Ether_HEADER_SIZE); //헤더를 붙인 데이터를 아래 레이어인 NILayer로 보냄

        m_sHeader.enet_data = null;
        return true;
    }

    public boolean SendFile(byte[] input, int length){
        m_sHeader.enet_type[0] = (byte) 0x20; //file라고 타입(2090) 지정
        m_sHeader.enet_type[1] = (byte) 0x90;
        m_sHeader.enet_data = input;
        byte[] buf = ObjToByte(m_sHeader, length);//헤더를 붙이는 함수 호출
        System.out.println("Ethernet - FileSend ");
        this.GetUnderLayer().Send(buf, length + Ether_HEADER_SIZE); //헤더를 붙인 데이터를 아래 레이어인 NILayer로 보냄

        m_sHeader.enet_data = null;
        return true;
    }

    public boolean Send(byte[] input, int length) {//아래 레이어인 NILayer에 데이터에 헤더를 붙여 전송하는 함수
        SendChat(input, length);
        return true;
    }


    public byte[] RemoveCappHeader(byte[] input, int length) {// receive할때 사용 / 헤더를 제거하는 함수
        byte[] buf = new byte[length - Ether_HEADER_SIZE];//배열에서 헤더 이후의 데이터만 옮겨서 리턴
        for (int i = 0; i < length - Ether_HEADER_SIZE; i++) {
            buf[i] = input[i + Ether_HEADER_SIZE];
        }
        return buf;
    }

    private boolean IsItMine(byte[] input) {
        for (int i = 0; i < 6; i++) {
            if (m_sHeader.enet_srcaddr.addr[i] == input[i]) //목적지이더넷주소가 자신의이더넷주소가아니면 false와 break
                continue;
            else {
                System.out.println("It isn't Mine");
                return false;
            }
        }
        System.out.println("It is Mine");
        return true;
    }

    public boolean IsItBroad(byte[] input) {
        for (int i = 0; i < 6; i++) {
            if (input[i] == (byte) 0xff) //목적지이더넷주소가 자신의이더넷주소가아니면 false와 break
                continue;
            else {
                System.out.println("It isn't Broad");
                return false;
            }
        }
        System.out.println("It is Broad");
        return true;
    }

    public boolean IsItMyPacket(byte[] input) {
        for (int i = 0; i < 6; i++) {
            if (m_sHeader.enet_srcaddr.addr[i] == input[i + 6]) //목적지이더넷주소가 자신의이더넷주소가아니면 false와 break
                continue;
            else {
                System.out.println("It isn't MyPacket");
                return false;
            }
        }
        System.out.println("It is MyPacket");
        return true;
    }


    public synchronized boolean Receive(byte[] input) {
        if(input.length>1500)
            return false;

        ChatAppLayer chatAppLayer = (ChatAppLayer) this.GetUpperLayer(0);
        FileAppLayer fileAppLayer = (FileAppLayer) this.GetUpperLayer(1);
        NILayer niLayer = (NILayer) this.GetUnderLayer();
        boolean MyPacket, Mine, Broadcast;
        MyPacket = IsItMyPacket(input);

        if (MyPacket == true) {
//            return false;
        } else {
            Broadcast = IsItBroad(input);
            if (Broadcast == false) {
                Mine = IsItMine(input);
                if (Mine == false) {
                    return false;
                }
            }
        }

        if (input[12] == (byte) 0x20 && input[13] == (byte) 0x80) {
            System.out.println("Ethernet - Receive" + new String(input));
            chatAppLayer.Receive(RemoveCappHeader(input, input.length));
            return true;
        }
        else if(input[12] == (byte) 0x20 && input[13] == (byte) 0x90){
            System.out.println("Ethernet - Receive");
            fileAppLayer.Receive(RemoveCappHeader(input, input.length));
            return true;
        }
//        else if (input[12] == (byte) 0x08 && input[13] == (byte) 0x02) {
//            chatAppLayer.isReceiveACK_Chat = true;
//            System.out.println("Ethernet - Ack 받음");
//            return true;
//        }
        return false;
    }



    private byte[] creatAck(byte[] input) {
        byte[] buf = new byte[Ether_HEADER_SIZE]; //input보다 14 큰 배열 선언
        System.arraycopy(m_sHeader.enet_dstaddr.addr, 0, buf, 0, 6); //배열의 0~5인덱스에 dst주소 붙임
        System.arraycopy(m_sHeader.enet_srcaddr.addr, 0, buf, 6, 6);//배열의 6~11인덱스에 src주소 붙임
        buf[12] = (byte) 0x08;
        buf[13] = (byte) 0x02;
        return buf;
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
