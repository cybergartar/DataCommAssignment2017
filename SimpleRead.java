import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;

import javax.comm.CommPortIdentifier;
import javax.comm.SerialPort;

public class SimpleRead {
    private static final char[]START = {'*', 'S', 'T', 'A', 'R', 'T', '*'};
    private static final  char[]COMMAND = {'*', 'R', 'D', 'Y', '*'};
    private static final int WIDTH = 320; //640;
    private static final int HEIGHT = 240; //480;

    private static CommPortIdentifier portCam, portRx, portTx;
    private InputStream inputStreamCam, inputStreamRx, inputStreamTx;
    private OutputStream outputStreamCam, outputStreamTx, outputStreamRx;
    private SerialPort serialPortCam, serialPortRx, serialPortTx;

    public static void main(String[] args) {
        Enumeration portList = CommPortIdentifier.getPortIdentifiers();
        Enumeration portListRx = CommPortIdentifier.getPortIdentifiers();
        Enumeration portListTx = CommPortIdentifier.getPortIdentifiers();

        while (portList.hasMoreElements()) {
            portCam = (CommPortIdentifier) portList.nextElement();
            if (portCam.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                System.out.println("Port name: " + portCam.getName());
                if (portCam.getName().equals("COM2")) {
                    break;
//                    SimpleRead reader = new SimpleRead();
                }
            }
        }

        while (portListRx.hasMoreElements()) {
            portRx = (CommPortIdentifier) portList.nextElement();
            if (portRx.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                System.out.println("Port name: " + portRx.getName());
                if (portRx.getName().equals("COM8")) {
//                    SimpleRead reader = new SimpleRead();
                    break;
                }
            }
        }

        while (portListTx.hasMoreElements()) {
            portTx = (CommPortIdentifier) portList.nextElement();
            if (portTx.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                System.out.println("Port name: " + portTx.getName());
                if (portTx.getName().equals("COM9")) {
                    new SimpleRead();
                }
            }
        }
    }

    public SimpleRead() {

        try {
            serialPortCam = (SerialPort) portCam.open("SimpleReadApp", 1000);
            inputStreamCam = serialPortCam.getInputStream();
            outputStreamCam = serialPortCam.getOutputStream();

            serialPortCam.setSerialPortParams(1000000,
                    SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);

            serialPortRx = (SerialPort) portRx.open("SimpleReadRx", 1000);
            inputStreamRx = serialPortRx.getInputStream();
            outputStreamRx = serialPortRx.getOutputStream();

            serialPortRx.setSerialPortParams(9600,
                    SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);

            serialPortTx = (SerialPort) portTx.open("SimpleReadTx", 1000);
            inputStreamTx = serialPortTx.getInputStream();
            outputStreamTx = serialPortTx.getOutputStream();

            serialPortTx.setSerialPortParams(9600,
                    SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);


            int counter = 0;

            while(!isBoardReady(inputStreamCam, 0)){};

            System.out.println("CAM READY!");
            char[] valSend = new char[3];

            while (true) {
                char inp = (char)read(inputStreamRx);
                while(!(inp == '#')){
                    System.out.println(inp);
                    inp = (char)read(inputStreamRx);
                };
                int cmm = read(inputStreamRx);
                if (cmm == 'T') {
                    int r = read(inputStreamRx);
                    if (r == '1') {
                        outputStreamCam.write(51);
                        outputStreamCam.write(49);
                    }
                    else if (r == '2') {
                        outputStreamCam.write(51);
                        outputStreamCam.write(50);
                    }
                    else if (r == '3') {
                        outputStreamCam.write(51);
                        outputStreamCam.write(51);
                    }
                }
                else if (cmm == 'I') {
                    sweepCapture(counter, valSend);
                }
                else if (cmm == 'R') {
                    int r = read(inputStreamRx);
                    char selectedValue = 0;
                    if (r == '1')
                        selectedValue = valSend[0];
                    else if (r == '2')
                        selectedValue = valSend[1];
                    else if (r == '3')
                        selectedValue = valSend[2];

                    sweepCapture(counter, valSend);

                    int min = 999, minAng = 3;
                    for (int i = 0; i < 3; i ++ ) {
                        int tempz = Math.abs(selectedValue - valSend[i]);
                        if (tempz < min) {
                            min = tempz;
                            minAng = i;
                        }
                    }

                    switch (minAng) {
                        case 0: outputStreamCam.write(51);
                                outputStreamCam.write(49);
                                break;

                        case 1: outputStreamCam.write(51);
                                outputStreamCam.write(49);
                                break;
                        case 2: outputStreamCam.write(51);
                                outputStreamCam.write(49);
                                break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sweepCapture (int counter, char[] valSend) throws IOException, InterruptedException{
        int countPic = 0;
        int[][]rgb = new int[HEIGHT][WIDTH];
        int[][]rgb2 = new int[WIDTH][HEIGHT];

        outputStreamCam.write('1');
        while(countPic < 3) {
            System.out.println();
            System.out.println("Looking for image");

            while(!isImageStart(inputStreamCam, 0)){};

            System.out.println("Found image: " + counter);

            int[] data = new int[HEIGHT];

            for (int y = 0; y < HEIGHT; y++) {
                for (int x = 0; x < WIDTH; x++) {
                    int temp = read(inputStreamCam);
                    rgb[y][x] = ((temp&0xFF) << 16) | ((temp&0xFF) << 8) | (temp&0xFF);
                    data[y] += temp;
                }
                data[y] /= WIDTH;
            }

            for (int y = 0; y < HEIGHT; y++) {
                for (int x = 0; x < WIDTH; x++) {
                    rgb2[x][y] = rgb[y][x];
                }
            }

            int average = 0;
            for (int i = 0; i < HEIGHT; i++) {
                average += data[i];
            }

            average = average / HEIGHT;

            System.out.println();
            System.out.println("Mean pixel: " + average);

            valSend[countPic] = (char) average;

            BMP bmp = new BMP();
            bmp.saveBMP("c:/out/" + (counter++) + "_" + average +".bmp", rgb2);

            System.out.println("Saved image: " + (counter-1));
            System.out.println();
            countPic++;
        }

        outputStreamCam.write('E');
        TimeUnit.SECONDS.sleep(5);
        outputStreamTx.write('$');

        for (int i = 0; i < 3; i ++) {
            outputStreamTx.write(valSend[i]);
        }
    }

    private int read(InputStream inputStream) throws IOException {
        int temp = (char) inputStream.read();
        if (temp == -1) {
            throw new  IllegalStateException("Exit");
        }
        return temp;
    }

    private boolean isBoardReady(InputStream inputStream, int index) throws IOException {
        if (index < START.length) {
            if (START[index] == read(inputStream)) {
                return isBoardReady(inputStream, ++index);
            } else {
                return false;
            }
        }
        return true;
    }

    private boolean isImageStart(InputStream inputStream, int index) throws IOException {
        if (index < COMMAND.length) {
            if (COMMAND[index] == read(inputStream)) {
                return isImageStart(inputStream, ++index);
            } else {
                return false;
            }
        }
        return true;
    }
}
