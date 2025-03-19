package zio2demo.storage.driver

import zio.{Chunk, IO, Ref}
import zio.test._
import zio.test.Assertion._
import zio.uuid.types.UUIDv7

object KeyValueStoreSpec extends ZIOSpecDefault {
  import zio2demo.model.{Company, Department, Employee, Entity}
  import zio2demo.model.ApplicationError.ApplicationError
  import zio2demo.common.Crypto._
  import zio2demo.model.EntityType._

  val company    = Company(
    UUIDv7.wrap(java.util.UUID.fromString("00000000-0000-0000-0000-000000000000")),
    "Company 1",
  )
  val department = Department(
    UUIDv7.wrap(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001")),
    "Department 1",
    company.id,
  )

  val employee_1 = Employee(
    UUIDv7.wrap(java.util.UUID.fromString("00000000-0000-0000-0000-000000000002")),
    "Joe Doe",
    "joe.doe@c.c",
    Crypto.hashPwd("joe-123"),
    department.id,
  )
  val employee_2 = Employee(
    UUIDv7.wrap(java.util.UUID.fromString("00000000-0000-0000-0000-000000000003")),
    "Jack Black",
    "jack.black@c.c",
    Crypto.hashPwd("jack-123"),
    department.id,
  )
  val employee_3 = Employee(
    UUIDv7.wrap(java.util.UUID.fromString("00000000-0000-0000-0000-000000000005")),
    "Jon Smith",
    "jon.smith@c.c",
    Crypto.hashPwd("jon-123"),
    department.id,
  )

  def spec = suite("KeyValueStoreSpec") {
    Ref
      .make(Map.empty[String, Map[UUIDv7, Entity]])
      .map((store: Ref[Map[String, Map[UUIDv7, Entity]]]) =>
        val kvl: KeyValueStore[ApplicationError, IO] = KeyValueStoreLive(store)
        zio.Chunk(
          suite("Adding value into empty store")(
            test("Should add company into store") {
              for {
                _ <- kvl.add[Company](company)
                companies <- store.get.map(_.get("companies").fold(Map.empty[UUIDv7, Company])(identity))
              } yield assert(companies)(hasSize(equalTo(1))) && assert(companies)(contains(company.id -> company))
            },
            test("Should add department into store") {
              for {
                _ <- kvl.add[Department](department)
                departments <- store.get.map(_.get("departments").fold(Map.empty[UUIDv7, Company])(identity))
              } yield assert(departments)(hasSize(equalTo(1))) && assert(departments)(contains(department.id -> department))
            },
            test("Should add employee into store") {
              for {
                _ <- kvl.add[Employee](employee_1)
                employees <- store.get.map(_.get("employees").fold(Map.empty[UUIDv7, Company])(identity))
              } yield assert(employees)(hasSize(equalTo(1))) && assert(employees)(contains(employee_1.id -> employee_1))
            }
          ),
          suite("Adding value into non empty store")(
            test("Should add employees into store") {
              for {
                _ <- kvl.add[Employee](employee_2) *> kvl.add[Employee](employee_3)
                employees <- store.get.map(_.get("employees").fold(Map.empty[UUIDv7, Employee])(identity))
              } yield assert(employees)(hasSize(equalTo(3))) && assert(employees)(hasSubset(Map(employee_2.id -> employee_2, employee_3.id -> employee_3)))
            }
          ),
          suite("Getting value from store")(
            test("Should get employee from store") {
              for {
                optEmployee <- kvl.get[Employee](employee_1.id)
              } yield assert(optEmployee)(isSome(equalTo(employee_1)))
            }
          ),
          suite("Getting values from store")(
            test("Should get all employees from store") {
              for {
                employees <- kvl.getAll[Employee]
              } yield assert(employees)(hasSize(equalTo(3))) && assert(employees)(hasSameElements(Seq(employee_1, employee_2, employee_3)))
            }
          ),
          suite("Removing values from store")(
            test("Should remove employee from store") {
              for {
                _ <- kvl.remove[Employee](employee_3.id)
                employees <- store.get.map(_.get("employees").fold(Map.empty[UUIDv7, Employee])(identity))
              } yield assert(employees)(hasSize(equalTo(2)) && hasSameElements(Map(employee_1.id -> employee_1, employee_2.id -> employee_2)))
            }
          )
        ),
      )
  } @@ TestAspect.sequential
}
