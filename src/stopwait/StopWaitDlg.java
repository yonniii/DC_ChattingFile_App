package stopwait;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import javafx.scene.control.ComboBox;
import javafx.scene.paint.Stop;
import org.jnetpcap.Pcap;
import org.jnetpcap.PcapIf;

public class StopWaitDlg extends JFrame implements BaseLayer {

    public int nUpperLayerCount = 0;
    public String pLayerName = null;
    public BaseLayer p_UnderLayer = null;
    public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
    NILayer nlLayer = (NILayer) m_LayerMgr.GetLayer("NI");
    EthernetLayer enetLayer = (EthernetLayer) m_LayerMgr.GetLayer("Ethernet");
    ChatAppLayer chatLayer = (ChatAppLayer) m_LayerMgr.GetLayer("ChatApp");
    FileAppLayer fileAppLayer = (FileAppLayer) m_LayerMgr.GetLayer("FileApp");

    BaseLayer UnderLayer;

    private static LayerManager m_LayerMgr = new LayerManager();

    private JTextField ChattingWrite;

    Container contentPane;

    JTextArea ChattingArea;
    JTextArea srcAddress;
    JTextArea dstAddress;

    JLabel lblsrc;
    JLabel lbldst;
    JLabel lblNIC;

    JButton Setting_Button;
    JButton Chat_send_Button;

    static JComboBox<String> NICComboBox;

    private JPanel fileNamePanel;
    private JTextField fileNameText;
    private JButton fileSendButton;
    private JPanel progressBarPanel;

    public JProgressBar progressBar;
    int adapterNumber = 0;

    String Text;

    public static void main(String[] args) {
        // TODO Auto-generated method stub
        m_LayerMgr.AddLayer(new NILayer("NI"));
        m_LayerMgr.AddLayer(new EthernetLayer("Ethernet"));
        m_LayerMgr.AddLayer(new ChatAppLayer("ChatApp"));
        m_LayerMgr.AddLayer(new FileAppLayer("FileApp"));
        m_LayerMgr.AddLayer(new StopWaitDlg("GUI"));// 레이어별로 객체를 생성하여 연결
        m_LayerMgr.ConnectLayers(" NI ( *Ethernet ( *ChatApp ( *GUI ) *FileApp ( *GUI ) ) ) ");


        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    StopWaitDlg frame = (StopWaitDlg) m_LayerMgr.GetLayer("GUI");
                    frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }

    public StopWaitDlg(String pName) {
        pLayerName = pName;

        setTitle("201701967_강서연");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(250, 250, 644, 425);
        contentPane = new JPanel();
        ((JComponent) contentPane).setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        contentPane.setLayout(null);

        JPanel chattingPanel = new JPanel();// chatting panel
        chattingPanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "chatting",
                TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        chattingPanel.setBounds(10, 5, 360, 276);
        contentPane.add(chattingPanel);
        chattingPanel.setLayout(null);

        JPanel chattingEditorPanel = new JPanel();// chatting write panel
        chattingEditorPanel.setBounds(10, 15, 340, 210);
        chattingPanel.add(chattingEditorPanel);
        chattingEditorPanel.setLayout(null);

        ChattingArea = new JTextArea();
        ChattingArea.setEditable(false);
        ChattingArea.setBounds(0, 0, 340, 210);
        chattingEditorPanel.add(ChattingArea);// chatting edit

        JPanel chattingInputPanel = new JPanel();// chatting write panel
        chattingInputPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
        chattingInputPanel.setBounds(10, 230, 250, 20);
        chattingPanel.add(chattingInputPanel);
        chattingInputPanel.setLayout(null);

        ChattingWrite = new JTextField();
        ChattingWrite.setBounds(2, 2, 250, 20);// 249
        chattingInputPanel.add(ChattingWrite);
        ChattingWrite.setColumns(10);// writing area

        JPanel settingPanel = new JPanel();
        settingPanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "setting",
                TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        settingPanel.setBounds(380, 5, 236, 371);
        contentPane.add(settingPanel);
        settingPanel.setLayout(null);

        NICComboBox = new JComboBox();
        NICComboBox.setBounds(10, 40, 170, 24);
        settingPanel.add(NICComboBox);
        lblNIC = new JLabel("NIC 선택");

        List<PcapIf> macList = nlLayer.m_pAdapterList; //연결된 mac주소들을 받아와 list에 저장
        for (int i = 0; i < macList.size(); i++) {
            NICComboBox.addItem(macList.get(i).getDescription());//콤보박스에 추가
        }


        NICComboBox.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    JComboBox jbox2 = (JComboBox) e.getItemSelectable();
                    int index = jbox2.getSelectedIndex();
                    byte byteData;
                    nlLayer.SetAdapterNumber(index);//선택된 콤보박스의 인덱스번호로 어답터넘버를 설정
                    try {
                        String tmp = "";
                        for (int i = 0; i < macList.get(index).getHardwareAddress().length; i++) {
                            byteData = macList.get(index).getHardwareAddress()[i];
//                            tmp += Integer.toString((byteData & 0xff)+0x100, 16).substring(1);
                            tmp += String.format("%02X%s", macList.get(index).getHardwareAddress()[i], (i < tmp.length() - 1) ? "" : "");
                            tmp += "-"; // mac주소를 byte type에서 string type으로 변환
                        }
                        System.out.println(tmp);
                        srcAddress.setText(tmp.substring(0, tmp.length() - 1)); // srcadress 창에 주소 띄움
                        macList.get(index).getHardwareAddress();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        });


        JPanel sourceAddressPanel = new JPanel();
        sourceAddressPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
        sourceAddressPanel.setBounds(10, 96, 170, 20);
        settingPanel.add(sourceAddressPanel);
        sourceAddressPanel.setLayout(null);

        lblsrc = new JLabel("Source Mac Address");
        lblsrc.setBounds(10, 75, 170, 20);
        settingPanel.add(lblsrc);

        srcAddress = new JTextArea();
        srcAddress.setBounds(2, 2, 170, 20);
        sourceAddressPanel.add(srcAddress);// src address

        JPanel destinationAddressPanel = new JPanel();
        destinationAddressPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
        destinationAddressPanel.setBounds(10, 212, 170, 20);
        settingPanel.add(destinationAddressPanel);
        destinationAddressPanel.setLayout(null);

        lbldst = new JLabel("Destination Mac Address");
        lbldst.setBounds(10, 187, 190, 20);
        settingPanel.add(lbldst);

        dstAddress = new JTextArea();
        dstAddress.setBounds(2, 2, 170, 20);
        destinationAddressPanel.add(dstAddress);// dst address
        Setting_Button = new JButton("Setting");// setting
        Setting_Button.setBounds(80, 270, 100, 20);
        Setting_Button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (Setting_Button.getText().equals("Setting")) {
                    srcAddress.setEnabled(false);
                    dstAddress.setEnabled(false);
                    Setting_Button.setText("Reset");
                    String src_ = srcAddress.getText();
                    String dst_ = dstAddress.getText();
                    byte[] srcaddr = new byte[6];
                    byte[] dstaddr = new byte[6];
                    for (int i = 0, j = 0; i < 17; i += 3, j++) {
                        dstaddr[j] = Integer.valueOf(dst_.substring(i, i + 2), 16).byteValue();
                        srcaddr[j] = Integer.valueOf(src_.substring(i, i + 2), 16).byteValue();
                    }
                    enetLayer.setSrcAddress(srcaddr); //세팅버튼 누르면 ethernetLayer에 src주소와 dst주소 설정
                    enetLayer.setDstAddress(dstaddr);
                } else {
                    srcAddress.setEnabled(true);
                    dstAddress.setEnabled(true);
                    Setting_Button.setText("Setting");
                }
            }
        });
        settingPanel.add(Setting_Button);// setting

        JLabel lbl_NIC = new JLabel("NIC \uC120\uD0DD");
        lbl_NIC.setBounds(14, 23, 62, 18);
        settingPanel.add(lbl_NIC);


        Chat_send_Button = new JButton("Send");
        Chat_send_Button.setBounds(270, 230, 80, 20);
        Chat_send_Button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (Setting_Button.getText().equals("Reset")) {
                    ChattingArea.append(new String("[SEND] :" + ChattingWrite.getText() + "\n")); // 보내려는 메세지를 출력
                    chatLayer.Send(ChattingWrite.getText().getBytes(), ChattingWrite.getText().length()); // 아래계층인 챗앱레이어에 데이터 보냄
                } else {
                    JOptionPane.showMessageDialog(null, "주소 설정 오류입니다.", "error", JOptionPane.WARNING_MESSAGE);
                }
            }
        });
        chattingPanel.add(Chat_send_Button);// chatting send button

        JPanel filepanel = new JPanel();
        filepanel.setBorder(
                new TitledBorder(null, "\uD30C\uC77C\uC804\uC1A1", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        filepanel.setBounds(10, 282, 360, 94);
        contentPane.add(filepanel);
        filepanel.setLayout(null);

        JButton fileNameButton = new JButton("\uD30C\uC77C\uC120\uD0DD");
        fileNameButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFrame window = new JFrame();
                JFileChooser fileChooser = new JFileChooser();

                int result = fileChooser.showOpenDialog(window);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    fileNameText.setText(selectedFile.toString());
                    System.out.println(selectedFile);
                }

            }
        });
        fileNameButton.setBounds(262, 23, 94, 20);
        filepanel.add(fileNameButton);

        fileNamePanel = new JPanel();
        fileNamePanel.setLayout(null);
        fileNamePanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
        fileNamePanel.setBounds(9, 24, 250, 20);
        filepanel.add(fileNamePanel);

        fileNameText = new JTextField();
        fileNameText.setColumns(10);
        fileNameText.setBounds(2, 2, 250, 20);
        fileNamePanel.add(fileNameText);

        fileSendButton = new JButton("\uC804\uC1A1");
        fileSendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String[] name = fileNameText.getText().split("\\\\");
                ChattingArea.append(new String("[SEND] :" + name[name.length - 1] + "을 전송합니다.\n"));
                fileAppLayer.Send(fileNameText.getText());
            }
        });
        fileSendButton.setBounds(262, 55, 94, 20);
        filepanel.add(fileSendButton);

        progressBarPanel = new JPanel();
        progressBarPanel.setLayout(null);
        progressBarPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
        progressBarPanel.setBounds(9, 55, 250, 20);
        filepanel.add(progressBarPanel);

        progressBar = new JProgressBar();
        progressBar.setBounds(0, 0, 250, 20);
        progressBarPanel.add(progressBar);

        setVisible(true);

    }

    public void setSenderProgressBar(int file_status) {
        progressBar.setValue(file_status);
    }

    void setReceiverProgressBar(int max, int current) {
        if ((int) ((float) current / (float) max * 100) == 100) {
            progressBar.setValue((int) ((float) current / (float) max * 100));
        } else {
            progressBar.setValue((int) ((float) current / (float) max * 100));
        }

    }

    public boolean Receive(byte[] input) {
        ChattingArea.append("[RECV] :" + new String(input) + "\n"); //input으로 받아온 데이터를 화면에 띄움

        return true;
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
