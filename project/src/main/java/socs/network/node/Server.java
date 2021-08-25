package socs.network.node;


import java.net.Socket;
import java.net.ServerSocket;
import java.io.IOException;


public class Server implements Runnable {
    //create a router object
   private Router router;

    //Server constractor
    public Server (Router router){
        this.router=router;
    }

    //create new thread
    public void run(){
        try{

            //make this Router run as server in this thread and open the socket to wait for attach request
            ServerSocket serverSocket =new ServerSocket(router.rd.processPortNumber);


            while(true){
                //serverSorket keeps listen to the client side
                Socket socket= serverSocket.accept();
                Thread client = new Thread(new ClientHandler(socket, router));
                client.start();
            }
        }catch (IOException ex){//detect any invalide input such as wrong user input
            System.err.println("ERROR WITH SERVER ROUTER");
        }
    }


}
