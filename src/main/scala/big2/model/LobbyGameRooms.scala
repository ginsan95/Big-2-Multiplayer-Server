package big2.model

import big2.util.ActorUtil;
import big2.controller.ClientActorInterface;
import akka.actor.{Actor,ActorRef,Address}
import scala.collection.mutable.{Set,HashSet,Map,HashMap,Buffer,ArrayBuffer};

import java.util.concurrent.ConcurrentHashMap;
import collection.JavaConverters._;

/**
 * @author AveryChoke
 */
//observer pattern
class LobbyGameRooms {
  
  //list of clients that subscribed to the server for new rooms update
  //or observing the updates on the game rooms
  val clientActors:Set[ActorRef] = new HashSet();
  //list of game rooms
  val gameRooms:Map[String, GameRoom] = new HashMap();
  
  def addActor(clientActor:ActorRef)
  {
    clientActors += clientActor;
  }
  
  def removeActor(clientActor:ActorRef)
  {
    clientActors -= clientActor;
  }
  
  def addGameRoom(room:GameRoom)
  {
    gameRooms += room.roomName -> room;
    broadcastGameRoom();
  }
  
  def removeGameRoom(roomName:String)
  {
    gameRooms -= roomName;
    broadcastGameRoom();
  }
  
  def addPlayerToRoom(roomName:String, clientActor:ActorRef):Option[ActorRef] =
  {
    val roomOption = gameRooms.get(roomName);
    
    //the room is found and available
    if(roomOption.isDefined && roomOption.get.available)
    {
      //add the player into the room
      roomOption.get.playerActors += clientActor;
      
      //remove client from subscriber list
      clientActors -= clientActor;
      
      //broadcast new updated game rooms
      broadcastGameRoom();
      //return the host client actor of the room
      return Some(roomOption.get.hostClientActor);
    }
    else
    {
      return Option(null);
    }
  }
  
  def removePlayerFromRoom(roomName:String, clientActor:ActorRef)
  {
    val roomOption = gameRooms.get(roomName);
    
    //the room is found
    if(roomOption.isDefined)
    {
      //add the player into the room
      roomOption.get.playerActors -= clientActor;
      
      //broadcast new updated game rooms
      broadcastGameRoom();
    }
  }
  
  def changeRoomVisibility(roomName:String, visible:Boolean)
  {
    val roomOption = gameRooms.get(roomName);
    
    //the room is found
    if(roomOption.isDefined)
    {
      //change visibilility
      roomOption.get.visible = visible;
      
      //broadcast new updated game rooms
      broadcastGameRoom();
    }
  }
  
  def roomHost(roomName:String):Option[ActorRef] = 
  {
    val roomOption = gameRooms.get(roomName);
    if(roomOption.isDefined)
    {
      return Some(roomOption.get.hostClientActor);
    }
    else
    {
      return Option(null);
    }
  }
  
  def roomPlayerActors(roomName:String):Option[Set[ActorRef]] =
  {
    val roomOption = gameRooms.get(roomName);
    if(roomOption.isDefined)
    {
      return Some(roomOption.get.playerActors);
    }
    else
    {
      return Option(null);
    }
  }
  
  //return a buffer with visible game room
  def visibleGameRooms:Buffer[GameRoom] ={
    val rooms:Buffer[GameRoom] = new ArrayBuffer();
    gameRooms.foreach {
      case (name, room) =>
        if(room.visible)
        {
          rooms += room;
        }
    }
    return rooms;
  }
  
  //broadcast rooms to all clients
  def broadcastGameRoom()
  {
    val rooms = visibleGameRooms;
    for(actor <- clientActors)
    {
      actor ! ClientActorInterface.ClientUpdateRoom(rooms);
    }
  }
  
  //tell if the room is exist
  def roomExist(roomName:String):Boolean =
  {
    return gameRooms.contains(roomName);
  }
}