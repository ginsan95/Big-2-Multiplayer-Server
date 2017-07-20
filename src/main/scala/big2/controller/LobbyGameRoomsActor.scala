package big2.controller

import big2.model.{GameRoom,LobbyGameRooms};
import big2.util.ActorUtil;

import akka.actor.{Actor,ActorRef,DeadLetter,Address}

/**
 * @author AveryChoke
 */
object LobbyGameRoomsActor {
  case class AddActor(clientActor:ActorRef);
  case class RemoveActor(clientActor:ActorRef);
  case class AddGameRoom(room:GameRoom);
  case class RemoveGameRoom(roomName:String);
  case class AddPlayerToRoom(roomName:String, clientActor:ActorRef);
  case class RemovePlayerFromRoom(roomName:String, clientActor:ActorRef);
  case class ChangeRoomVisibility(roomName:String, visible:Boolean);
  
  case class GetRoomHost(roomName:String);
  case class GetRoomPlayerActors(roomName:String);
  case object GetVisibleGameRooms;
  case object GetClientActors;
}

import LobbyGameRoomsActor._;

class LobbyGameRoomsActor extends Actor {
  
  //object that is observed by clients
  private var gameRooms:LobbyGameRooms = null;
  
  override def preStart = {
    gameRooms = new LobbyGameRooms();
  }
  
  override def receive = {
    case AddActor(clientActor) =>
      gameRooms.addActor(clientActor);
    case RemoveActor(clientActor) =>
      gameRooms.removeActor(clientActor);
    case AddGameRoom(room) =>
      gameRooms.addGameRoom(room);
    case RemoveGameRoom(roomName) =>
      gameRooms.removeGameRoom(roomName);
    case AddPlayerToRoom(roomName, clientActor) =>
      sender ! gameRooms.addPlayerToRoom(roomName, clientActor);
    case RemovePlayerFromRoom(roomName, clientActor) =>
      gameRooms.removePlayerFromRoom(roomName, clientActor);
    case ChangeRoomVisibility(roomName, visible) =>
      gameRooms.changeRoomVisibility(roomName, visible);
    case GetRoomHost(roomName) =>
      sender ! gameRooms.roomHost(roomName);
    case GetRoomPlayerActors(roomName:String) =>
      sender ! gameRooms.roomPlayerActors(roomName);
    case GetVisibleGameRooms =>
      sender ! gameRooms.visibleGameRooms;
    case GetClientActors =>
      sender ! gameRooms.clientActors;
  }
  
}