package redis.clients.jedis.tests;

import org.junit.Test;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ConnectionBrokenDeterminerTest {

  private static HostAndPort hp = HostAndPortUtil.getRedisServers().get(0);

  @Test
  public void testCustomExceptionHandlerShouldMarkConnectionAsBroken() {
    JedisPool jedisPool = new JedisPool(new JedisPoolConfig(), hp.getHost(), hp.getPort(), 2000, "foobared");

    ConnectionBrokenDeterminer determiner = new ConnectionBrokenDeterminer();
    determiner.addPattern(new ConnectionBrokenPattern() {
      @Override
      public boolean determine(Connection connection, RuntimeException throwable) {
        // It covers "Wrong number of args calling Redis command From Lua script"
        if (throwable instanceof JedisDataException) {
          JedisDataException e = (JedisDataException) throwable;
          if (e.getMessage().contains("Wrong number of args")) {
            return true;
          }
        }
        return false;
      }
    });

    jedisPool.setConnectionBrokenDeterminer(determiner);
    Jedis jedis = jedisPool.getResource();

    try {
      jedis.eval("return redis.pcall('hset', 'a', 'b')");
      fail("Should raise JedisConnectionException");
    } catch (JedisConnectionException e) {
      assertTrue(jedis.getClient().isBroken());
    } catch (Throwable e) {
      fail("Should handle certain exception to JedisConnectionException");
    } finally {
      jedis.close();
      jedisPool.close();
    }
  }
}
