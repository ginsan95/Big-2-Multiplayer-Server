package big2

import big2.controller.ServerActor;
import akka.actor._
import scala.io.StdIn;
import com.typesafe.config._
import java.net.InetAddress;

/**
 * @author AveryChoke
 */
object Application extends App {
  
  private val SERVER_ADDRESS = "p4big2.ddns.net";
  private val localAddress = InetAddress.getLocalHost.getHostAddress;
  
  //choices to deploy server
  println("Select 1 of the options below to deploy your server\n" +
      "1 - Default server global address (p4big2.ddns.com)\n" +
      "2 - My own local address\n" +
      "3 - Custom address");
  print("Enter choice here: ");
  val choice = StdIn.readLine();
  
  private var hostname:String = null;
  
  choice match {
    case "2" =>
      hostname = localAddress;
    case "3" =>
      print("Enter your custom address here: ");
      hostname = StdIn.readLine();
    case _ => //else everything go to the default choice
      hostname = SERVER_ADDRESS;
  }
  
  //create new conf
  val overrideConf = ConfigFactory.parseString(s"akka.remote.netty.tcp.hostname=$hostname," + 
      s"akka.remote.netty.tcp.bind-hostname=$localAddress");
  val myConf = overrideConf.withFallback(ConfigFactory.load());
  
  //create the actor system
  var actorSystem:ActorSystem = ActorSystem("Big2System", myConf);
  //create the server actor
  val serverActor = actorSystem.actorOf(Props[ServerActor], "ServerActor");
  //get the lobby game rooms actor
  val lobbyGameRoomsActor = actorSystem.actorSelection("/user/ServerActor/LobbyGameRoomsActor");
}