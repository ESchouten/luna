/*
 A bootstrapper acting as the "master dependency" for all other plugins. All of the complex, high level, 'dirty work'
 is done in this plugin in order to ensure that other plugins can be written as idiomatically as possible.

 The interception of posted events can be handled through the 'on' functions. One to intercept events with no matching
 and another to intercept with matching (if the arguments given are in agreement with the 'matches' method in the Event
 class).

 Please note that normal functions and fields must come before event interception.

 Also, because this plugin acts as a master dependency great caution needs to be taken when modifying its
 contents. Changing and/or removing the wrong thing could result in breaking every plugin.

 AUTHOR: lare96
*/

import java.util.Optional
import java.util.concurrent.{Callable, ThreadLocalRandom, TimeUnit}
import java.util.function._

import io.luna.game.event.{Event, EventListener}
import io.luna.game.model.`def`.ItemDefinition
import io.luna.game.model.item.{Item, ItemContainer, RationalItem}
import io.luna.game.model.mobile._
import io.luna.game.model.mobile.attr.AttributeValue
import io.luna.game.model.mobile.update.UpdateFlagHolder.UpdateFlag
import io.luna.game.model.{Entity, EntityType, Position, World}
import io.luna.game.plugin.PluginFailureException
import io.luna.game.task.Task
import io.luna.net.msg.out._
import io.luna.util.{Rational, StringUtils}

import scala.collection.JavaConversions._
import scala.reflect.ClassTag
import scala.util.Random


/* Aliases for 'LunaContext'. */
def plugins = ctx.getPlugins
def world = ctx.getWorld
def service = ctx.getService


/* Aliases for 'PlayerRights'. */
val RIGHTS_PLAYER = PlayerRights.PLAYER
val RIGHTS_MOD = PlayerRights.MODERATOR
val RIGHTS_ADMIN = PlayerRights.ADMINISTRATOR
val RIGHTS_DEV = PlayerRights.DEVELOPER


/* Aliases for 'EntityType'. */
val TYPE_PLAYER = EntityType.PLAYER
val TYPE_NPC = EntityType.NPC
val TYPE_OBJECT = EntityType.OBJECT
val TYPE_ITEM = EntityType.ITEM


/* Aliases for 'Skill'. */
val SKILL_ATTACK = Skill.ATTACK
val SKILL_DEFENCE = Skill.DEFENCE
val SKILL_STRENGTH = Skill.STRENGTH
val SKILL_HITPOITNS = Skill.HITPOINTS
val SKILL_RANGED = Skill.RANGED
val SKILL_PRAYER = Skill.PRAYER
val SKILL_MAGIC = Skill.MAGIC
val SKILL_COOKING = Skill.COOKING
val SKILL_WOODCUTTING = Skill.WOODCUTTING
val SKILL_FLETCHING = Skill.FLETCHING
val SKILL_FISHING = Skill.FISHING
val SKILL_FIREMAKING = Skill.FIREMAKING
val SKILL_CRAFTING = Skill.CRAFTING
val SKILL_SMITHING = Skill.SMITHING
val SKILL_MINING = Skill.MINING
val SKILL_HERBLORE = Skill.HERBLORE
val SKILL_AGILITY = Skill.AGILITY
val SKILL_THIEVING = Skill.THIEVING
val SKILL_SLAYER = Skill.SLAYER
val SKILL_FARMING = Skill.FARMING
val SKILL_RUNECRAFTING = Skill.RUNECRAFTING


/* Aliases for 'RationalItem'. */
val CHANCE_ALWAYS = RationalItem.ALWAYS
val CHANCE_VERY_COMMON = RationalItem.VERY_COMMON
val CHANCE_COMMON = RationalItem.COMMON
val CHANCE_UNCOMMON = RationalItem.UNCOMMON
val CHANCE_VERY_UNCOMMON = RationalItem.VERY_UNCOMMON
val CHANCE_RARE = RationalItem.RARE
val CHANCE_VERY_RARE = RationalItem.VERY_RARE


/* Implicit conversions. */
implicit def javaToScalaRange(range: com.google.common.collect.Range[java.lang.Integer]): Range = {
  failIf(!range.hasLowerBound || !range.hasUpperBound,
    "Conversion from Range[Integer] to Range requires a lower and upper bound")

  val lower = range.lowerEndpoint.toInt
  val upper = range.upperEndpoint.toInt

  lower to upper
}

implicit def javaToScalaOptional[T <: Optional[T]](optional: T): Option[T] = {
  if (optional.isPresent) {
    Some(optional.get)
  } else {
    None
  }
}

// TODO Remove when Scala 2.12 becomes stable in the coming months
implicit def javaToScalaConsumer[T](consumer: Consumer[T]): T => Unit
= (t: T) => consumer.accept(t)

implicit def javaToScalaBiConsumer[T, U](biConsumer: BiConsumer[T, U]): (T, U) => Unit
= (t: T, u: U) => biConsumer.accept(t, u)

implicit def javaToScalaFunction[T, R](function: Function[T, R]): T => R
= (t: T) => function.apply(t)

implicit def javaToScalaBiFunction[T, U, R](biFunction: BiFunction[T, U, R]): (T, U) => R
= (t: T, u: U) => biFunction.apply(t, u)

implicit def javaToScalaPredicate[T](predicate: Predicate[T]): T => Boolean
= (t: T) => predicate.test(t)

implicit def javaToScalaBiPredicate[T, U](biPredicate: BiPredicate[T, U]): (T, U) => Boolean
= (t: T, u: U) => biPredicate.test(t, u)

implicit def javaToScalaSupplier[T](supplier: Supplier[T]): () => T
= () => supplier.get

implicit def scalaToJavaConsumer[T](func: T => Unit): Consumer[T] = new Consumer[T] {
  override def accept(t: T) = func(t)
}

implicit def scalaToJavaBiConsumer[T, U](func: (T, U) => Unit): BiConsumer[T, U] = new BiConsumer[T, U] {
  override def accept(t: T, u: U) = func(t, u)
}

implicit def scalaToJavaFunction[T, R](func: T => R): Function[T, R] = new Function[T, R] {
  override def apply(t: T) = func(t)
}

implicit def scalaToJavaBiFunction[T, U, R](func: (T, U) => R): BiFunction[T, U, R] = new BiFunction[T, U, R] {
  override def apply(t: T, u: U) = func(t, u)
}

implicit def scalaToJavaPredicate[T](func: T => Boolean): Predicate[T] = new Predicate[T] {
  override def test(t: T) = func(t)
}

implicit def scalaToJavaBiPredicate[T, U](func: (T, U) => Boolean): BiPredicate[T, U] = new BiPredicate[T, U] {
  override def test(t: T, u: U) = func(t, u)
}

implicit def scalaToJavaSupplier[T](func: () => T): Supplier[T] = new Supplier[T] {
  override def get = func()
}


/* Random generation functions. */
def rand = ThreadLocalRandom.current
def rand[E](seq: Seq[E]) = seq((rand.nextDouble * seq.length).toInt)
def rand[E](arr: Array[E]) = arr((rand.nextDouble * arr.length).toInt)
def rand(fromInclusive: Int = 0, toInclusive: Int) = rand.nextInt((toInclusive - fromInclusive) + 1) + fromInclusive


/* Retrieves the system time in 'MILLISECONDS'. */
def currentTimeMillis = TimeUnit.MILLISECONDS.convert(System.nanoTime, TimeUnit.NANOSECONDS)


/* Retrieves the name of an item. */
def nameOfItem(id: Int) = {
  val name = ItemDefinition.computeNameForId(id)
  if (name.isPresent) {
    name.get
  } else {
    fail("no name found for 'id'")
  }
}
def nameOfItem(item: Item) = nameOfItem(item.getId)


/* Appends an article between a prefix and suffix. */
def joinArticle(prefix: String, thing: String, suffix: String) = {
  val sb = new StringBuilder
  val article = StringUtils.computeArticle(thing)
  val space = ' '

  sb ++= prefix += space ++= article += space ++= thing += space ++= suffix

  sb.toString
}


/* Logging and printing functions. */
def log(msg: Any) = logger.info(s"$msg")
def logIf(cond: Boolean, msg: => Any) = if (cond) { log(msg) }


/* Failure and assertion functions. */
def fail(msg: Any = "[failure]: no reason specified") = throw new PluginFailureException(s"$msg")
def failIf(cond: Boolean, msg: => Any = "[failure]: cond == false") = if (cond) { fail(msg) }


/* Normal event interception function. No matching happens here. */
def on[E <: Event](eventListener: E => Unit)
  (implicit tag: ClassTag[E]): Unit =
  pipelines.addEventListener(tag.runtimeClass, new EventListener(eventListener))

/*
 Event interception function, with matching on parameters. This method requires that at least one argument
 is specified, in order to avoid conflicts with the previous "on" method.
*/
def on[E <: Event](arg1: Any, argOther: Any*)
  (eventListener: E => Unit)
  (implicit tag: ClassTag[E]): Unit = {

  val argList = (List(argOther) ++ List(arg1)).map(_.asInstanceOf[AnyRef])

  val newEventListener = (msg: E) => {
    if (msg.matches(argList: _*)) {
      eventListener(msg)
      msg.terminate
      return
    }
  }

  pipelines.addEventListener(tag.runtimeClass, new EventListener(newEventListener))
}


/* Asynchronous block functions. */
def async(func: => Unit) = service.submit(new Runnable {
  override def run() = {
    try {
      func
    } catch {
      case e: Exception => e.printStackTrace()
    }
  }
})

def async[T](func: => T) = service.submit(new Callable[T] {
  override def call = func
})


/*
 All implicit (monkey patching) classes below are 'extending' Java classes by creating new functions for them. We
 do this to ensure that all code coming from Java is as concise and idiomatic (Scala-like) as possible.

 Feel free to add on to the existing code, but beware:
  -> Removing code will (most likely) break existing plugins
  -> Functions that are too ambiguous may end up doing more harm than good (their purpose
     should be relatively obvious)
*/
implicit class RichPlayer(plr: Player) {

  def address = plr.getSession.getHostAddress
  def name = plr.getUsername
  def rights = plr.getRights

  def bank = plr.getBank
  def inventory = plr.getInventory
  def equipment = plr.getEquipment

  def sendMessage(message: String) = plr.queue(new GameChatboxMessageWriter(message))
  def sendWidgetText(text: String, widget: Int) = plr.queue(new WidgetTextMessageWriter(text, widget))
  def sendForceTab(id: Int) = plr.queue(new ForceTabMessageWriter(id))
  def sendChatboxInterface(id: Int) = plr.queue(new ChatboxInterfaceMessageWriter(id))
  def sendSkillUpdate(id: Int) = plr.queue(new SkillUpdateMessageWriter(id))
  def sendMusic(id: Int) = plr.queue(new MusicMessageWriter(id))
  def sendSound(id: Int, loops: Int = 0, delay: Int = 0) = plr.queue(new SoundMessageWriter(id, loops, delay))
  def sendInterface(id: Int) = plr.queue(new InterfaceMessageWriter(id))
  def sendConfig(id: Int, value: Int) = plr.queue(new ConfigMessageWriter(id, value))
  def sendColor(id: Int, color: Int) = plr.queue(new ColorChangeMessageWriter(id, color))
  def sendItemModel(id: Int, scale: Int, item: Int) = plr.queue(new WidgetItemModelMessageWriter(id, scale, item))
  def sendTabInterface(tab: Int, interface: Int) = plr.queue(new TabInterfaceMessageWriter(tab, interface))

  def flag(updateFlag: UpdateFlag) = plr.getUpdateFlags.flag(updateFlag)

  def stopWalking = plr.getWalkingQueue.clear()

  def lockMovement = {
    stopWalking
    plr.getWalkingQueue.setLocked(true)
  }
  def unlockMovement = plr.getWalkingQueue.setLocked(false)
}


implicit class RichMobileEntity(mob: MobileEntity) {

  def attr[T](key: String): T = {
    val attr: AttributeValue[T] = mob.getAttributes.get(key)
    attr.get
  }

  def attr[T](key: String, value: T) = {
    val attr: AttributeValue[T] = mob.getAttributes.get(key)
    attr.set(value)
  }

  def attrEquals(key: String, equals: Any) = equals == attr(key)

  def elapsedTime(key: String, ms: Long) = {
    val value: Long = attr(key)
    (currentTimeMillis - value) >= ms
  }

  def resetTime(key: String) = attr(key, currentTimeMillis)

  def walking = mob.getWalkingQueue
  def skills = mob.getSkills
}


implicit class RichPlayerRights(rights: PlayerRights) {

  def <=(other: PlayerRights) = rights.equalOrLess(other)
  def >=(other: PlayerRights) = rights.equalOrGreater(other)
  def >(other: PlayerRights) = rights.greater(other)
  def <(other: PlayerRights) = rights.less(other)
  def ==(other: PlayerRights) = rights.equal(other)
}


implicit class RichEntity(entity: Entity) {

  def position = entity.getPosition
  def x = position.getX
  def y = position.getY
  def z = position.getZ
}


implicit class RichWorld(world: World) {

  def addNpc(npc: Npc): Npc = {
    world.getNpcs.add(npc)
    npc
  }
  def addNpc(id: Int, position: Position): Npc = addNpc(new Npc(ctx, id, position))

  def schedule(delay: Int, instant: Boolean = false)(action: Task => Unit) =
    world.schedule(new Task(instant, delay) {
      override protected def execute() = {
        action(this)
      }
    })

  def scheduleOnce(delay: Int)(action: => Unit) = {
    schedule(delay) { task =>
      action
      task.cancel()
    }
  }

  def scheduleTimes(delay: Int, times: Int)(action: => Unit) = {
    var loops = 0
    schedule(delay) { task =>
      if (loops == times) {
        task.cancel()
      } else {
        action
        loops += 1
      }
    }
  }

  def scheduleInterval(range: Range)(action: Task => Unit) = {
    schedule(rand(range)) { task =>
      action(task)
      task.setDelay(rand(range))
    }
  }

  def scheduleUntil(delay: Int, predicate: Boolean)(action: => Unit) = {
    schedule(delay) { task =>
      if (!predicate) {
        action
      } else {
        task.cancel()
      }
    }
  }

  def scheduleForever(delay: Int, instant: Boolean = false)(action: => Unit) = {
    schedule(delay, instant) { task =>
      action
    }
  }

  def messageToAll(str: String) = world.getPlayers.foreach(_.sendMessage(str))
}


implicit class RichArray[T](array: Array[T]) {

  def shuffle = {
    var i = array.length - 1
    while (i > 0) {
      val index = rand(i)
      val a = array(index)
      array(index) = array(i)
      array(i) = a
      i -= 1
    }
    array
  }
}


implicit class RichItemContainer(items: ItemContainer) {
  def getIdForIndex(index: Int) = items.computeIdForIndex(index).orElse(-1)
}


implicit class RichSeq[T](seq: Seq[T]) {
  def shuffle = Random.shuffle(seq)
}


implicit class RichTraversable[T](traversable: Traversable[T]) {

  def lazyFilter(pred: T => Boolean) = traversable.withFilter(pred)
  def lazyFilterNot(pred: T => Boolean) = traversable.withFilter(it => !pred(it))
}


implicit class RichInt(int: Int) {

  def times(action: => Unit) = {
    failIf(int < 0, "int value must be >= 0")

    var times = 0
    while (times < int) {
      times += 1
      action
    }
  }

  def of(denominator: Int) = new Rational(int, denominator)
}
