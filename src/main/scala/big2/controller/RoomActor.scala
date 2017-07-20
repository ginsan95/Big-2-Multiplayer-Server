package big2.controller

import big2.Application;
import big2.model.{GameRoom,LobbyGameRooms};
import big2.util.ActorUtil;

import akka.actor.{Actor,ActorRef,ActorSelection, DeadLetter,Address}
import scala.collection.mutable.{Buffer,ArrayBuffer,Map,HashMap,Set};
import akka.remote.{DisassociatedEvent}
import akka.util.Timeout
import akka.pattern.ask

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.control.Breaks;

import ClientActorInterface._;
import LobbyGameRoomsActor._;

/**
 * @author AveryChoke
 */
object RoomActor {
  case class HostGame(clientActor:ActorRef, sender:ActorRef);
  case class JoinRoom(clientActor:ActorRef, sender:ActorRef);
  case object StartGame;
  case class LeaveRoom(sender:ActorRef);
  case object ReturnRoom;
}

import RoomActor._;

class RoomActor(roomName:String) extends Actor{
  
  private var lobbyGameRoomsActor:ActorSelection = null;
  private implicit val timeout: Timeout = 10.second;
  
  override def preStart = {
    //get the lobby game rooms actor
    lobbyGameRoomsActor = Application.lobbyGameRoomsActor;
    
    //subscribe to error
    context.system.eventStream.subscribe(self, classOf[DisassociatedEvent]);
  }
  
  override def receive = {
    case HostGame(clientActor, sender) => hostGame(clientActor, sender);
    case JoinRoom(clientActor, sender) => joinRoom(clientActor, sender);
    case StartGame => startGame();
    case LeaveRoom(sender) => leaveRoom(sender);
    case ReturnRoom => returnRoom();
    
    //fault handling - handle fault for own room
    case DisassociatedEvent(localAddress, remoteAddress, inbound) =>
      disconnectPlayer(remoteAddress);
  }
  
  private def hostGame(clientActor:ActorRef, mySender:ActorRef)
  {
    //create room
    val newRoom = new GameRoom(roomName, clientActor);
    
    //remove client from subscriber list
    lobbyGameRoomsActor ! RemoveActor(clientActor);
    
    //inform client his room name
    mySender ! roomName;
    
    //add the new room into the list - auto broadcast
    lobbyGameRoomsActor ! AddGameRoom(newRoom);
  }
  
  private def joinRoom(clientActor:ActorRef, mySender:ActorRef)
  { 
    val successFuture = (lobbyGameRoomsActor ? AddPlayerToRoom(roomName, clientActor));
    
    successFuture.foreach {
      case hostClient:Option[ActorRef] =>
        //successfully added the player into the room - auto broadcast
        if(hostClient.isDefined)
        {
          //send back client allow him to join the host
          mySender ! hostClient.get;
        }
        else
        {
          //inform client unavailable
          mySender ! "UNAVAILABLE";
        }
    }
  }
  
  private def startGame()
  {
    //change the room to not visible - auto broadcast
    lobbyGameRoomsActor ! ChangeRoomVisibility(roomName, false);
  }
  
  private def returnRoom()
  {
    //change the room to visible - auto broadcast
    lobbyGameRoomsActor ! ChangeRoomVisibility(roomName, true);
  }
  
  private def leaveRoom(mySender:ActorRef)
  {
    //subscribe the player
    lobbyGameRoomsActor ! AddActor(mySender);
    
    //remove the leaving player
    removePlayer(mySender);
  }
  
  //sub method to perform the removing
  private def removePlayer(removingActor:ActorRef)
  {
    //get the host of the room
    val hostFuture = lobbyGameRoomsActor ? GetRoomHost(roomName);
    
    hostFuture.foreach {
      case hostOption:Option[ActorRef] =>
        //if there is host means the room is available
        if(hostOption.isDefined)
        {
          //if host is the player who is going to be removed
          if(hostOption.get == removingActor)
          {
            //remove the game room - auto broadcast
            lobbyGameRoomsActor ! RemoveGameRoom(roomName);
            
            //tell server actor to kill the room actor cause the room is not longer there
            Application.serverActor ! ServerActor.RemoveRoomActor(roomName);
          }
          else
          {
            //remove the player from the room - auto broadcast
            lobbyGameRoomsActor ! RemovePlayerFromRoom(roomName, removingActor)
          }
        }
    }
  }
  
  private def disconnectPlayer(clientActorAddress:Address)
  {
    //get the player actors of the room
    val roomActorsFuture = lobbyGameRoomsActor ? GetRoomPlayerActors(roomName);
    
    roomActorsFuture.foreach {
      case roomActorsOption:Option[Set[ActorRef]] =>
        if(roomActorsOption.isDefined)
        {
          //find actor 
          val actor:Option[ActorRef] = ActorUtil.searchActorRefByAddress(clientActorAddress,
              roomActorsOption.get);
          
          if(actor.isDefined)
          {
            //remove the disconnected player
            removePlayer(actor.get);
          }
        }
    }
  }
}