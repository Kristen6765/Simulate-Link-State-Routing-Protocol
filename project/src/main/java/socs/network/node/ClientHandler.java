package socs.network.node;

import socs.network.message.SOSPFPacket;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable  {
    Router router;
    Socket socket;

    //clientHandler constractor
    public ClientHandler(Socket socket, Router router){
        this.router=router;
        this.socket=socket;
    }

    public void helperAttach(SOSPFPacket recivedRequest, ObjectOutputStream outStream, ObjectInputStream inStream){

        //check if has been already connected to the incoming Router
        //if yes, don't link again
        boolean ifExists= false;
        for(int i=0; i< 4; i++){
            if (router.ports[i]!=null &&
                    router.ports[i].router2.simulatedIPAddress.equals(recivedRequest.srcIP)){
                ifExists=true;
                System.out.println("CONNECTION EXIST");
                break;
            }
        }
        //if linked does not exist, then connect
        if(ifExists==false){

            int portNum=-1;
            for (int i = 0; i < 4; i++) {
                if (router.ports[i] == null) {//make sure there are free port to connect
                    RouterDescription routerDescription2 = new RouterDescription();
                    routerDescription2.processIPAddress = recivedRequest.srcProcessIP;
                    routerDescription2.simulatedIPAddress = recivedRequest.srcIP;
                    routerDescription2.processPortNumber = recivedRequest.srcProcessPort;
                    router.ports[i] = new Link(router.rd, routerDescription2, recivedRequest.HelloWeight);
                    portNum=i;
                    break;
                }
            }



            System.out.println("received HELLO from " + recivedRequest.srcIP + ";");
            //change status of server/client
            router.ports[portNum].router2.status=RouterStatus.INIT;
            router.ports[portNum].router1.status=RouterStatus.INIT;

            System.out.println("set " + recivedRequest.srcIP + " state to INIT");

            //create a packet and initialize it
            SOSPFPacket helloMsg = new SOSPFPacket();
            helloMsg.srcProcessIP =  router.ports[portNum].router2.processIPAddress;
            helloMsg.srcProcessPort =   router.ports[portNum].router2.processPortNumber;
            helloMsg.srcIP =   router.ports[portNum].router1.simulatedIPAddress;
            helloMsg.dstIP =   router.ports[portNum].router2.simulatedIPAddress;
            helloMsg.sospfType = 0; //0 - HELLO, 1 - LinkState Update
            helloMsg.neighborID =   router.ports[portNum].router2.simulatedIPAddress;
            helloMsg.routerID =   router.ports[portNum].router2.simulatedIPAddress;

            //send msg
            try {
                outStream.writeObject(helloMsg);
            } catch (IOException e) {
                System.err.println("IOException:  " + e);
            }





            //wait for response
            try {
                recivedRequest= (SOSPFPacket)inStream.readObject();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }


            //check if the response is HELLO
            if (recivedRequest == null || (recivedRequest.sospfType != 0 && recivedRequest.sospfType != 1)) {
                System.out.println("Error: invalide msg");
                return;
            }

            System.out.println("received HELLO from " + recivedRequest.srcIP + ";");
            router.ports[portNum].router1.status = RouterStatus.TWO_WAY;
            System.out.println("set " + recivedRequest.srcIP + " state to TWO_WAY");
            router.ports[portNum].router2.status = RouterStatus.TWO_WAY;



        }





    }


    public void run() {
        try{
            //create in/output stream to end and receive msg
            ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());
            ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());


            //receive request
            Object request =inStream.readObject();
            //check if resquest is empty
            if (request==null) {
                System.err.println("EMPTY REQUEST");
                return;
            }else{



                //create an object of SOSPFPacket
                SOSPFPacket recivedRequest= (SOSPFPacket) request;

                //if the IPaddress of attach command is the same of server IPaddress in config
                if(!recivedRequest.dstIP.equals(router.rd.simulatedIPAddress)){
                	System.err.println("simulatedIPAddress is incorrect");
                	System.err.println("recivedRequest.dstIP"+recivedRequest.dstIP);
                	System.err.println("router.rd.simulatedIPAddress"+router.rd.simulatedIPAddress);
                	
                    
                }

                switch(recivedRequest.sospfType){
                    case 0://HELLO
                        helperAttach(recivedRequest,outStream,inStream);
                        break;
                    case 1://LinkState Update
                        break;

                    default:
                        System.out.println("Error: invalide msg");
                        break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
