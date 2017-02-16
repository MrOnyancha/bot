package models

import com.github.jreddit.parser.entity.Submission
import play.api.libs.json.Json

import scala.concurrent.Future

case class User(id: String)

case class Message(mid: Option[String] = None, seq: Option[Long] = None, text: String)

case class Delivery(mids: Seq[String], watermark: Long, seq: Long)

object User {
  implicit val writes = Json.writes[User]
  implicit val reads = Json.reads[User]
}

object Message {
  implicit val writes = Json.writes[Message]
  implicit val reads = Json.reads[Message]
}

object Delivery {
  implicit val writes = Json.writes[Delivery]
  implicit val reads = Json.reads[Delivery]
}

case class Messaging(
                      sender: User,
                      recipient: User,
                      timestamp: Option[Long] = None,
                      message: Option[Message] = None,
                      delivery: Option[Delivery] = None
                      //  postback:
                      // read:
                      // account_linking:
                      //optin:
                    )

object Messaging {
  implicit val writes = Json.writes[Messaging]
  implicit val reads = Json.reads[Messaging]
}

case class Entry(id: String, time: Long, messaging: Seq[Messaging])

object Entry {
  implicit val writes = Json.writes[Entry]
  implicit val reads = Json.reads[Entry]
}

/**
  * Represents a message received from Messenger Platform. It has the following structure:
  *
  * {{{
  *   {
  *     "object":"page",
  *     "entry":[
  *       {
  *         "id":"PAGE_ID",
  *         "time":1460245674269,
  *         "messaging":[
  *           {
  *             "sender":{
  *               "id":"USER_ID"
  *             },
  *             "recipient":{
  *               "id":"PAGE_ID"
  *             },
  *             "timestamp":1460245672080,
  *             "message":{
  *               "mid":"mid.1460245671959:dad2ec9421b03d6f78",
  *               "seq":216,
  *               "text":"hello"
  *             }
  *           }
  *         ]
  *       }
  *     ]
  *   }
  * }}}
  *
  * @param `object` the facebook object from where the message is being sent
  * @param entry    the messages
  */
case class ReceivedMessage(`object`: String, entry: Seq[Entry])

object ReceivedMessage {
  implicit val writes = Json.writes[ReceivedMessage]
  implicit val reads = Json.reads[ReceivedMessage]
}

/**
  * A simple text message that follow the structure below:
  *
  * {{{
  *   {
  *     "recipient": {
  *       "id": 12345
  *     },
  *     "message": {
  *        "text": "The message text"
  *     }
  *   }
  * }}}
  *
  * This is used to send very simple messages to the user, such as greetings and
  * also instructions about how to use the bot.
  *
  * @param recipient the user that will receive the message
  * @param message   the message itself
  */
case class TextResponse(recipient: User, message: Message)

object TextResponse {
  implicit val writes = Json.writes[TextResponse]
  implicit val reads = Json.reads[TextResponse]
}


case class Button(`type`: String = "web_url", title: String, url: String)

object Button {
  implicit val writes = Json.writes[Button]
  implicit val reads = Json.reads[Button]
}

case class Card(title: String, subtitle: String, image_url: Option[String], buttons: Seq[Button])

object Card {
  implicit val writes = Json.writes[Card]
  implicit val reads = Json.reads[Card]
}

case class Payload(template_type: String = "generic", elements: Seq[Card])

object Payload {
  implicit val writes = Json.writes[Payload]
  implicit val reads = Json.reads[Payload]
}

case class Attachment(`type`: String = "template", payload: Payload)


/**
  * The structured message to send rich content like bubbles/cards. It has the following structure:
  *
  * {{{
  *   {
  *     "recipient": {
  *       "id": "USER_ID"
  *     },
  *     "message": {
  *         "attachment": {
  *             "type": "template",
  *             "payload": {
  *                 "template_type": "generic",
  *                 "elements": [
  *                     {
  *                         "title": "First card",
  *                         "subtitle": "Element #1 of an hscroll",
  *                         "image_url": "http://messengerdemo.parseapp.com/img/rift.png",
  *                         "buttons": [
  *                             {
  *                                 "type": "web_url",
  *                                 "url": "https://www.messenger.com/",
  *                                 "title": "Web url"
  *                             },
  *                             {
  *                                 "type": "postback",
  *                                 "title": "Postback",
  *                                 "payload": "Payload for first element in a generic bubble"
  *                             }
  *                         ]
  *                     },
  *                     {
  *                         "title": "Second card",
  *                         "subtitle": "Element #2 of an hscroll",
  *                         "image_url": "http://messengerdemo.parseapp.com/img/gearvr.png",
  *                         "buttons": [
  *                             {
  *                                 "type": "postback",
  *                                 "title": "Postback",
  *                                 "payload": "Payload for second element in a generic bubble"
  *                             }
  *                         ]
  *                     }
  *                 ]
  *             }
  *         }
  *     }
  *   }
  * }}}
  *
  * @param recipient the user that will receive the message
  * @param message   the message with attachment
  */
case class StructuredMessage(recipient: User, message: Map[String, Attachment])


case class Error(
                  message: String,
                  `type`: String,
                  code: Long,
                  fbtrace_id: String
                )

object Attachment {
  def from(posts: Seq[Submission]): Attachment = {
    val cards = posts.take(10).map { post =>
      Card(
        title = post.getTitle,
        subtitle = s"From ${post.getAuthor} | ${post.getCommentCount} comments | ${post.getUpVotes} ups | ${post.getDownVotes} downs",
        image_url = (if (post.getSource != null) Some(post.getSource.getUrl) else None).map(_.replaceAll("amp;", "")),
        buttons = buttons(post)
      )
    }
    Attachment(payload = Payload(elements = cards))
  }

  private def buttons(post: Submission): Seq[Button] = {
    val buttons = Seq[Button](Button(title = "Open link", url = post.getURL))
    if (post.getURL.contains("reddit.com/r/"))
      buttons
    else
      buttons :+ Button(title = "Reddit conversation", url = "https://www.reddit.com" + post.getPermalink)
  }

  implicit val writes = Json.writes[Attachment]
  implicit val reads = Json.reads[Attachment]
}

object Messages {
  def typingMessage(sender: User) = Future( Json.obj(
    "recipient" -> Json.obj("id" -> sender.id),
    "sender_action" -> "typing_on"
  ))


  implicit val userFormat = Json.format[User]
  implicit val messageFormat = Json.format[Message]
  implicit val deliveryFormat = Json.format[Delivery]
  implicit val messagingFormat = Json.format[Messaging]
  implicit val entryFormat = Json.format[Entry]
  implicit val receivedMessageFormat = Json.format[ReceivedMessage]

  implicit val textResponseFormat = Json.format[TextResponse]
  implicit val buttonFormat = Json.format[Button]
  implicit val cardFormat = Json.format[Card]
  implicit val payloadFormat = Json.format[Payload]
  implicit val attachmentFormat = Json.format[Attachment]
  implicit val structuredMessageFormat = Json.format[StructuredMessage]

  implicit val errorFormat = Json.format[Error]

  lazy val commandPattern = "/?([a-zA-Z0-9_]+)/(hot|top|new|controversial|rising)".r

  def help(sender: User) = TextResponse(sender, message = Message(text =
    """
      | Welcome to clinicPesa services. Please choose the Number in the 
      | 1. help
      | 2. /subreddit/order where order is "hot", "top", "new", "controversial" or "rising".
      |
      | Some examples:
      | 1. /food/hot
      | 2. /science/top
    """.stripMargin))

  def oops(sender: User, cause: String) = TextResponse(sender, message = Message(text =
    s"""
       | You know, robots fails sometimes. Unfortunately I was not able to get reddit posts. :-(
       |
    | Here is what happen:
       | $cause
       |
    | You can try again later.
  """.stripMargin))
}