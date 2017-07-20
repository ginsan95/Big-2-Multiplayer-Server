package big2.controller

import big2.model.{GameRoom,LobbyGameRooms};
import big2.util.ActorUtil;

import akka.actor.{Actor,ActorRef,DeadLetter,Address,Props}
import scala.collection.mutable.{Buffer,ArrayBuffer,Map,HashMap,Set};
import akka.remote.{DisassociatedEvent}
import akka.util.Timeout
import akka.pattern.ask

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.control.Breaks;

import ServerActorInterface._;
import ClientActorInterface._;
import LobbyGameRoomsActor._;
import RoomActor._;

/**
 * @author AveryChoke
 */
object ServerActor {
  case class RemoveRoomActor(roomName:String);
}

import ServerActor._;

class ServerActor extends Actor{
  
  //the lobby game rooms actor
  private var lobbyGameRoomsActor:ActorRef = null;
  //a list of room actors - thread per object
  private val roomActors:Map[String,ActorRef] = new HashMap();
  
  private var counter = 1000;
  private implicit val timeout: Timeout = 20.second;
  
  override def preStart = {
    lobbyGameRoomsActor = context.actorOf(Props[LobbyGameRoomsActor], "LobbyGameRoomsActor");
    
    //subscribe to error
    context.system.eventStream.subscribe(self, classOf[DisassociatedEvent]);
  }
  
  override def receive = {
    case ServerSubscribe => subscribe(sender);
    case ServerHostGame(roomName, clientActor) => hostGame(roomName, clientActor);
    case ServerJoinRoom(roomName, clientActor) => joinRoom(roomName, clientActor);
    case ServerStartGame(roomName) => startGame(roomName);
    case ServerLeaveRoom(roomName) => leaveRoom(roomName);
    case ServerReturnRoom(roomName) => returnRoom(roomName);
    case RemoveRoomActor(roomName) => removeRoomActor(roomName);
    
    //fault handling
    //case DeadLetter(message, sender, recipient) => unsubscribeServer(recipient);
    case DisassociatedEvent(localAddress, remoteAddress, inbound) =>
      //unsubscribe the player
      unsubscribePlayer(remoteAddress)
  }
  
  private def subscribe(clientActor:ActorRef)
  {
    //add client to subscriber
    lobbyGameRoomsActor ! AddActor(clientActor);
    
    val realSender = sender;
    
    //inform client the list of rooms
    val future = lobbyGameRoomsActor ? GetVisibleGameRooms;
    future.foreach {
      case rooms:Buffer[GameRoom] =>
        realSender ! ClientUpdateRoom(rooms);
    }
  }
  
  private def hostGame(roomName:String, clientActor:ActorRef)
  {
    //avoid conflicting name
    val newRoomName = changeName(roomName);
    
    //create new room actor
    val roomActor = context.actorOf(Props(classOf[RoomActor], newRoomName),
          name = String.valueOf(counter));
    counter += 1;
    
    //add actor into the list
    roomActors += newRoomName -> roomActor;
    
    //tell room actor to host game
    roomActor ! HostGame(clientActor, sender);
  }
  
  private def joinRoom(roomName:String, clientActor:ActorRef)
  { 
    //get the room actor
    val roomOption = roomActors.get(roomName);
    
    if(roomOption.isDefined)
    {
      //tell actor to join room
      roomOption.get ! JoinRoom(clientActor, sender);
    }
    else
    {
      //inform client unavailable
      sender ! "UNAVAILABLE";
    }
  }
  
  private def startGame(roomName:String)
  {
    //get the room actor
    val roomOption = roomActors.get(roomName);
    if(roomOption.isDefined)
    {
      //tell actor to start game
      roomOption.get ! StartGame;
    }
  }
  
  private def returnRoom(roomName:String)
  {
    //get the room actor
    val roomOption = roomActors.get(roomName);
    if(roomOption.isDefined)
    {
      //tell actor to return room
      roomOption.get ! ReturnRoom;
    }
  }
  
  private def leaveRoom(roomName:String)
  {
    //get the room actor
    val roomOption = roomActors.get(roomName);
    if(roomOption.isDefined)
    {
      //tell actor to leave room
      roomOption.get ! LeaveRoom(sender);
    }
  }
  
  private def removeRoomActor(roomName:String)
  {
    //remove the actor from the list
    val actorOption = roomActors.remove(roomName);
    
    //kill the actor
    if(actorOption.isDefined)
    {
      context.stop(actorOption.get);
    }
  }
  
  //change the room name
  private def changeName(name:String):String =
  {
    var count = 1;
    var newName = name;
    while(roomActors.contains(newName))
    {
      newName = s"$name-$count";
      count += 1;
    }
    return newName;
  }
  
  //search for the actor and unsubscribe it
  private def unsubscribePlayer(clientActorAddress:Address)
  {
    //get the subscribed players
    val playersFuture = lobbyGameRoomsActor ? GetClientActors;
    
    playersFuture.foreach {
      case clientActors:Set[ActorRef] =>
        
        val actor:Option[ActorRef] = ActorUtil.searchActorRefByAddress(clientActorAddress,
          clientActors);
    
        if(actor.isDefined)
        {
          lobbyGameRoomsActor ! RemoveActor(actor.get);
        }
    }
  }
}