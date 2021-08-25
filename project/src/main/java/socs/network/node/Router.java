package socs.network.node;

import socs.network.util.Configuration;
import socs.network.message.*;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.net.ServerSocket;



public class Router {

  protected LinkStateDatabase lsd;

  RouterDescription rd = new RouterDescription();

  //assuming that all routers are with 4 ports
  Link[] ports = new Link[4];
  ServerSocket listenSocket = null;


  public Router(Configuration config) {
    rd.simulatedIPAddress = config.getString("socs.network.router.ip");
    rd.processPortNumber=config.getShort("socs.network.router.port");

    //assign the process IP to 127.0.0.1
    try {
      rd.processIPAddress = java.net.InetAddress.getLocalHost().getHostAddress();


    } catch (UnknownHostException e) {
      System.err.println("Host IP does not exist");
    }


    lsd = new LinkStateDatabase(rd);


  }

  /**
   * output the shortest path to the given destination ip
   * <p/>
   * format: source ip address  -> ip address -> ... -> destination ip
   *
   * @param destinationIP the ip adderss of the destination simulated router
   */
  private void processDetect(String destinationIP) {

  }

  /**
   * disconnect with the router identified by the given destination ip address
   * Notice: this command should trigger the synchronization of database
   *
   * @param portNumber the port number which the link attaches at
   */
  private void processDisconnect(short portNumber) {

  }

  /**
   * attach the link to the remote router, which is identified by the given simulated ip;
   * to establish the connection via socket, you need to indentify the process IP and process Port;
   * additionally, weight is the cost to transmitting data through the link
   * <p/>
   * NOTE: this command should not trigger link database synchronization
   */
  private void processAttach(String processIP, short processPort,
                             String simulatedIP, short weight) {

    //can't attach to self
    if(this.rd.processIPAddress.equals(processIP) && rd.processPortNumber==processPort){
      System.err.println("CAN'T ATTCH TO YOUSELF");
    }

    if(this.rd.simulatedIPAddress.equals(simulatedIP)){
      System.err.println("CAN'T ATTCH TO YOUSELF");
    }




    //create RouterDescription for the coming Router
    RouterDescription routerDescription2= new RouterDescription();

    //attach
    for(int i=0; i< ports.length; i++){
      if(ports[i]!=null && ports[i].router2.simulatedIPAddress.equals(simulatedIP)){
        System.err.println("ALLREADY ATTACHED TO THIS ROUTER");
        return;
      }
      if (ports[i] == null){ //attach rd to router2
        routerDescription2.simulatedIPAddress= processIP;
        routerDescription2.processPortNumber= processPort;
        routerDescription2.simulatedIPAddress=simulatedIP;
        ports[i]=new Link(rd,routerDescription2,weight);
        break;
      }

      //i==3 => all ports are occupied
      if(i==3){
        System.err.println("No more ports available!");
        return;
      }
    }

  }

  /**
   * broadcast Hello to neighbors
   */
  private void processStart() {
    //test if current Rounter has attached to another rounter, if not we can't send msg
    boolean notAttach= true;
    int i=0;
    for (Link link : this.ports) {
      if (link != null) {
        notAttach=false; //notAttach=false => Rounter has attched to as least one Router
      }
    }
    if(notAttach){
      System.err.println("NOT ATTACHED YET");
      return;
    }

    Socket clientSocket = null;
    ObjectOutputStream output = null;
    ObjectInputStream input = null;

    //iterate through ports arrary to send msg to neighbours
    for (Link link : ports) {
      if (link == null) {
        continue;
      }


      try {

        clientSocket = new Socket( ports[i].router2.processIPAddress,(short)ports[i].router2.processPortNumber);
        output = new ObjectOutputStream(clientSocket.getOutputStream());
        input = new ObjectInputStream(clientSocket.getInputStream());
      } catch (IOException e) {
        System.out.println("err connect to : "+ ports[i].router2.processIPAddress+" " +(short)ports[i].router2.processPortNumber+" "+ports[i].router2.simulatedIPAddress);
        continue;
      }


      //create a packet and initialize it
      SOSPFPacket helloMsg = new SOSPFPacket();
      helloMsg.srcProcessIP = link.router1.processIPAddress;
      helloMsg.srcProcessPort = link.router1.processPortNumber;
      helloMsg.srcIP = link.router1.simulatedIPAddress;
      helloMsg.dstIP = link.router2.simulatedIPAddress;
      helloMsg.sospfType = 0; //0 - HELLO, 1 - LinkState Update
      helloMsg.neighborID = link.router1.simulatedIPAddress;
      helloMsg.routerID = link.router1.simulatedIPAddress;

      //send msg
      try {
        output.writeObject(helloMsg);
      } catch (IOException e) {
        System.err.println("IOException:  " + e);
      }

      //wait for response
      SOSPFPacket responseMsg = new SOSPFPacket();
      try {
        responseMsg = (SOSPFPacket) input.readObject();
      } catch (IOException e) {
        System.err.println("IOException:  " + e);
      } catch (ClassNotFoundException e) {
        System.err.println("Corrupted packet");
        return;
      }

      //check if the response is HELLO
      if (responseMsg == null || (responseMsg.sospfType != 0 && responseMsg.sospfType != 1) ) {
        System.out.println("Error: invalide msg");
        return;
      }


      System.out.println("received HELLO from " + responseMsg.srcIP + ";");
      ports[i].router1.status = RouterStatus.TWO_WAY;

      //send back the HELLO packet (second time)
      try {
        output.writeObject(helloMsg);
      } catch (IOException e) {
        e.printStackTrace();
      }

      ports[i].router2.status = RouterStatus.TWO_WAY;
      System.out.println("set " + responseMsg.srcIP + " state to TWO_WAY");



      i++;
    }


  }

  /**
   * attach the link to the remote router, which is identified by the given simulated ip;
   * to establish the connection via socket, you need to indentify the process IP and process Port;
   * additionally, weight is the cost to transmitting data through the link
   * <p/>
   * This command does trigger the link database synchronization
   */
  private void processConnect(String processIP, short processPort,
                              String simulatedIP, short weight) {




  }

  /**
   * output the neighbors of the routers
   */
  private void processNeighbors() {
    int i=0;
    //for (Link link: this.ports){
    for (i=0;i<this.ports.length;i++){
      if(ports[i]!=null){
        System.out.println("IP address of neighbor " + (i + 1) + ": " + ports[i].router2.simulatedIPAddress);
        
      }
    }

  }


  /**
   * disconnect with all neighbors and quit the program
   */
  private void processQuit() {

  }

  public void terminal() {
    try {
      InputStreamReader isReader = new InputStreamReader(System.in);
      BufferedReader br = new BufferedReader(isReader);
      System.out.print(">> ");
      String command = br.readLine();
      while (true) {
        try {
          if (command.startsWith("detect ")) {
            String[] cmdLine = command.split(" ");
            processDetect(cmdLine[1]);
          } else if (command.startsWith("disconnect ")) {
            String[] cmdLine = command.split(" ");
            processDisconnect(Short.parseShort(cmdLine[1]));
          } else if (command.startsWith("quit")) {
            processQuit();
            break;
          } else if (command.startsWith("attach ")) {
            String[] cmdLine = command.split(" ");
            processAttach(cmdLine[1], Short.parseShort(cmdLine[2]),
                    cmdLine[3], Short.parseShort(cmdLine[4]));
          } else if (command.equals("start")) {
            processStart();
          } else if (command.equals("connect ")) {
            String[] cmdLine = command.split(" ");
            processConnect(cmdLine[1], Short.parseShort(cmdLine[2]),
                    cmdLine[3], Short.parseShort(cmdLine[4]));
          } else if (command.equals("neighbors")) {
            //output neighbors
            processNeighbors();
          } else {
            //invalid command
            System.out.println("invalid command");
            //break;
          }
        } catch(Exception e) {
          System.out.println("err ");
        }
        System.out.print(">> ");
        command = br.readLine();
      }
      isReader.close();
      br.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }



}
