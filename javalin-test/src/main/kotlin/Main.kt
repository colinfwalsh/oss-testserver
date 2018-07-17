import io.javalin.Javalin
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.dao.*

object Users : IntIdTable() {
    val name = varchar("name", 50).index()
    val city = reference("city", Cities)
    val age = integer("age")
}

object Cities: IntIdTable() {
    val name = varchar("name", 50)
}

class User(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<User>(Users)

    var name by Users.name
    var city by City referencedOn Users.city
    var age by Users.age
}

class City(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<City>(Cities)

    var name by Cities.name
    val users by User referrersOn Users.city
}

fun main(args: Array<String>) {
    val app = Javalin.start(7000)
    // app.get("/") { ctx -> ctx.result("This is a test") }
    app.get("/test-endpoint/*") {ctx ->
        ctx.result("test-endpoint")
    }

    //calls before("/*", handler)
    // could be useful for modifying data before sending
    //app.before {ctx -> }

    app.post("/") {
        ctx ->
        ctx.status(201)
    }

    //Very useful method for passing in data in order to return a specific result
    app.get("/hello/:name") {ctx ->
        ctx.result("Hello: " + ctx.param("name"))
    }
    //Useful for multi parameter arguments
    app.get("hello/*/and/*") {ctx ->
        ctx.result("Hello: " + ctx.splat(0) + " and " + ctx.splat(1))
    }

    Database.connect("jdbc:h2:mem:test", driver = "org.h2.Driver")

    transaction {
        logger.addLogger(StdOutSqlLogger)

        create (Cities, Users)

        val stPete = City.new {
            name = "St. Petersburg"
        }

        val munich = City.new {
            name = "Munich"
        }

        User.new {
            name = "a"
            city = stPete
            age = 5
        }

        User.new {
            name = "b"
            city = stPete
            age = 27
        }

        User.new {
            name = "c"
            city = munich
            age = 42
        }

        println("Cities: ${City.all().joinToString {it.name}}")
        println("Users in ${stPete.name}: ${stPete.users.joinToString {it.name}}")
        println("Adults: ${User.find { Users.age greaterEq 18 }.joinToString {it.name}}")
    }
}