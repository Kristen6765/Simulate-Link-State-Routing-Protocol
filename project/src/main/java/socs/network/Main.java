package socs.network;

import socs.network.node.Router;
import socs.network.node.Server;
import socs.network.util.Configuration;

public class Main {

  public static void main(String[] args) {
    if (args.length != 1) {
      System.out.println("usage: program conf_path");
      System.exit(1);
    }
    Router r = new Router(new Configuration(args[0]));

    //create a new thread to listen as a server
    Thread server = new Thread(new Server(r));
    server.start();

    System.out.println("initiating terminal");

    r.terminal();


  }
}
