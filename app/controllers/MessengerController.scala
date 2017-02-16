package controllers

import javax.inject.{Inject, Singleton}

import models._
import play.api.libs.ws.WSClient

import scala.concurrent.Future

//import models._
//import models.Messages._
import play.api.Configuration
import play.api.libs.json._
import play.api.mvc.{Action, Controller}
import services.{MessengerService, RedditService}

import scala.concurrent.ExecutionContext

@Singleton
class MessengerController @Inject()(
                                     ws: WSClient,
                                     messengerService: MessengerService,
                                     config: Configuration,
                                     redditService: RedditService
                                   )(implicit executionContext: ExecutionContext) extends Controller {

  def verifyApp = Action { implicit request =>
    val expectedToken = config
      .getString("facebook.app.verifyToken")
      .getOrElse(sys.error("Configuration `facebook.app.verifyToken` was not found."))

    println(s" THE REQUEST:  ${request.body.asJson.toList.toString()}")
    request.getQueryString("hub.verify_token") match {
      case Some(verifyToken) =>
        val challenge = request.getQueryString("hub.challenge").getOrElse("")
        if (verifyToken == expectedToken) Ok(challenge) else Forbidden
      case None => Unauthorized
    }
  }

  def receiveMessage = Action.async(parse.json) { request =>
    Json.fromJson[ReceivedMessage](request.body) match {
      case JsSuccess(obj, _) => println(s"testing out the $obj"); Future(Ok("SENT"))
//    println(s"testing out the ${request.body}"); Future(Ok("SENT"))
      case JsError(x) => println(s"fail $x"); Future(Ok("SENT"))
    }
  }


//    def receiveMessage = Action.async(parse.json) { request =>
//    val futures = request.body.as[ReceivedMessage].entry
//      .flatMap(_.messaging)
//      .filter(_.message.isDefined)
//      .map(messaging => messaging.sender -> messaging.message.get)
//      .map { tuple =>
//        val sender = tuple._1
//        val message = tuple._2
//        message.text match {
//          case Messages.commandPattern(subreddit, order) => println(subreddit, order); Future(Json.toJson(Messages.help(sender))) // getRedditPosts(subreddit, order, sender)
//          case _ => Future(Json.toJson(Messages.help(sender)))
//        }
//      }
//      .map(messengerService.reply)
//    Future.sequence(futures).map(responses => Ok("Finished"))
//  }

//  private def getRedditPosts(subreddit: String, order: String, sender: User): Future[JsValue] = {
//    redditService.getSubreddit(subreddit, order, Some(10)).map {
//      posts =>
//        Json.toJson(
//          StructuredMessage(
//            recipient = sender,
//            message = Map("attachment" -> Attachment.from(posts))
//          )
//        )
//    }
//
//  }


}


/* def receiveMessage = Action(parse.tolerantJson) { req =>
   val data = req.body

   (data \ "object").as[String] match {
     // Make sure this is a page subscription
     case "page" =>
       // Iterate over each entry
       // There may be multiple if batched
       /**
         * {{{
         * {
         *   "object":"page",
         *   "entry":[
         *     {
         *       "id":"PAGE_ID",
         *       "time":1458692752478,
         *       "messaging":[
         *         {
         *           "sender":{
         *             "id":"USER_ID"
         *           },
         *           "recipient":{
         *             "id":"PAGE_ID"
         *           },
         *
         *           ...
         *         }
         *       ]
         *     }
         *   ]
         * }
         * }}}
         */
       val entries = (data \ "entry").as[List[JsValue]]
       println(s"TESTING THE ENTRY: $entries ")
       entries.foreach { pageEntry =>
         val pageID = (pageEntry \ "id").as[Long]
         val timeOfEvent = (pageEntry \ "time").as[Long]

         val messaging = (pageEntry \ "messaging").as[List[JsObject]]
         messaging.foreach { messagingEvent =>
           receivedMessage(messagingEvent)
         }
       }

       // Assume all went well.
       //
       // You must send back a 200, within 20 seconds, to let us know you've
       // successfully received the callback. Otherwise, the request will time out.
       Status(200)
     case _ =>
       Status(403)
   }
 }
*/


/*
   if (messagingEvent.optin) {
        receivedAuthentication(messagingEvent);
      } else if (messagingEvent.message) {
        receivedMessage(messagingEvent);
      } else if (messagingEvent.delivery) {
        receivedDeliveryConfirmation(messagingEvent);
      } else if (messagingEvent.postback) {
        receivedPostback(messagingEvent);
      } else if (messagingEvent.read) {
        receivedMessageRead(messagingEvent);
      } else if (messagingEvent.account_linking) {
        receivedAccountLink(messagingEvent);
      } else {
        console.log("Webhook received unknown messagingEvent: ", messagingEvent);
      }
*/


/* private def receivedMessage(event: JsObject) = {

   val senderID = (event \ "sender" \ "id").as[String]
   val recipientID = (event \ "recipient" \ "id").as[String]

   val maybeMessage = (event \ "message").asOpt[JsObject]
   val maybeDelivery = (event \ "delivery").asOpt[JsObject]
   val maybeRead = (event \ "read").asOpt[JsObject]
   val postBack = (event \ "postback").asOpt[JsObject]

   if (postBack.nonEmpty) {
     sendTextMessage(senderID, postBack.get.fields.head.toString())
   }

   println {
     "Received message for user %s and page %s with message: %s".format(senderID, recipientID, maybeMessage)
   }

   maybeMessage.foreach { message =>
     (message \ "text").asOpt[String].foreach { messageText =>
       sendTextMessage(senderID, messageText)
     }
   }

   //    maybeDelivery.foreach { delivery =>
   //      (delivery \ "watermark").asOpt[String].foreach { deliveryText =>
   //        sendTextMessage(senderID, "Delivered")
   //      }
   //    }
   //
   //
   //    maybeRead.foreach { read =>
   //      (read \ "watermark").asOpt[String].foreach { readText =>
   //        sendTextMessage(senderID, "Read")
   //      }
   //    }
 }

 def sendTextMessage(recipientID: String, messageText: String) = {
   val ACCESS_TOKEN = config.getString("facebook.messages.token").getOrElse("")

   ws.url(config.getString("facebook.messages.url")
     .getOrElse("https://graph.facebook.com/v2.6/me/messages"))
     .withQueryString("access_token" -> ACCESS_TOKEN)
     .post(Json.obj(
       //        Json.arr
       "recipient" -> Json.obj("id" -> recipientID),
       "message" -> Json.obj(
         "attachment" -> Json.obj(
           "type" -> "template",
           "payload" -> Json.obj(
             "template_type" -> "button",
             "text" -> "Please Login",
             "buttons" ->  Json.arr(
               Json.obj(
                 "type" -> "account_link",
                 "url" -> "https://app.clinicpesa.com"
               )
             )
           )
         )
       )
     )
     )

 }
*/


/*def sendTextMessage(recipientID: String, messageText: String) = {
    val ACCESS_TOKEN = config.getString("facebook.messages.token").getOrElse("")

    ws.url(config.getString("facebook.messages.url")
      .getOrElse("https://graph.facebook.com/v2.8/me/messages"))
      .withQueryString("access_token" -> ACCESS_TOKEN)
      .post(Json.obj(
        //        Json.arr
        "recipient" -> Json.obj("id" -> recipientID),
        "message" -> Json.obj(
          "attachment" -> Json.obj(
            "type" -> "template",
            "payload" -> Json.obj(
              "template_type" -> "generic",
              "elements" -> Json.arr(Json.obj(
                "title" -> "rift",
                "subtitle" -> "Next-generation virtual reality",
                "item_url" -> "https://www.oculus.com/en-us/rift/",
                "image_url" -> "http://messengerdemo.parseapp.com/img/rift.png",
                "buttons" -> Json.arr(Json.obj(
                  "type" -> "web_url",
                  "url" -> "https://www.oculus.com/en-us/rift/",
                  "title" -> "Open Web URL"
                ), Json.obj(
                  "type" -> "postback",
                  "title" -> "Call Postback",
                  "payload" -> "Payload for first bubble"
                ))
              ), Json.obj(
                "title" -> "touch",
                "subtitle" -> "Your Hands, Now in VR",
                "item_url" -> "https://www.oculus.com/en-us/touch/",
                "image_url" -> "http://messengerdemo.parseapp.com/img/touch.png",
                "buttons" -> Json.arr(Json.obj(
                  "type" -> "web_url",
                  "url" -> "https://www.oculus.com/en-us/touch/",
                  "title" -> "Open Web URL"
                ), Json.obj(
                  "type" -> "postback",
                  "title" -> "Call Postback",
                  "payload" -> "Payload for second bubble"
                ))
              ))
            )
          )
        )
      ))
  }
*/