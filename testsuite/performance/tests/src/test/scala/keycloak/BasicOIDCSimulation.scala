package keycloak

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import keycloak.CommonScenarioBuilder._
import keycloak.BasicOIDCScenarioBuilder._

import org.keycloak.performance.TestConfig


/**
  * @author Radim Vansa &lt;rvansa@redhat.com&gt;
  * @author Marko Strukelj &lt;mstrukel@redhat.com&gt;
  */
class BasicOIDCSimulation extends Simulation {

  println()
  println("Target servers: " + TestConfig.serverUrisList)
  println()

  println("Using test parameters:\n" + TestConfig.toStringCommonTestParameters);
  println("  refreshTokenCount: " + TestConfig.refreshTokenCount)
  println("  badLoginAttempts: " + TestConfig.badLoginAttempts)
  println()
  println("Using dataset properties:\n" + TestConfig.toStringDatasetProperties)


  val httpDefault = http
    .acceptHeader("application/json")
    .disableFollowRedirect
    .inferHtmlResources

  val userSession = new BasicOIDCScenarioBuilder()

      .browserOpensLoginPage()

      .thinkPause()
      .browserPostsWrongCredentials()
      .browserPostsCorrectCredentials()

      // Act as client adapter - exchange code for keys
      .adapterExchangesCodeForTokens()

      .refreshTokenSeveralTimes()

      .thinkPause()
      .logout()

      .thinkPause()


  val usersScenario = scenario("users").exec(userSession.chainBuilder)

  setUp(usersScenario
    .inject(
       rampUsersPerSec(0.001) to TestConfig.usersPerSec during(TestConfig.rampUpPeriod),
       constantUsersPerSec(TestConfig.usersPerSec) during(TestConfig.warmUpPeriod + TestConfig.measurementPeriod) 
    ).protocols(httpDefault))

  after {
    filterResults(getClass)
  }

}
