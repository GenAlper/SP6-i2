import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import Bleach.Level.Level;

public class EnemySpawner implements Runnable {
	private AtomicInteger numberOfSquidsAlive = new AtomicInteger(0);
	private Random rand = new Random();
	private ExecutorService squidAI = Executors.newFixedThreadPool(4);
	private Player player;
	private Level level;

	public EnemySpawner(Player player, Level level) {
		this.player = player;
		this.level = level;
		squidAI.execute(this);
	}

	@Override
	public void run() {

		while (true) {

			if (rand.nextDouble() > 0.20)
				try {
					int sleepTime = rand.nextInt(10000) + 1000;
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			else if (numberOfSquidsAlive.get() < 3)
				spawnSquidoe();
		}

	}

	public void spawnSquidoe() {
		numberOfSquidsAlive.incrementAndGet();

		double randomX = player.getPosition().getX() - rand.nextInt(500) - 100;
		double randomY = player.getPosition().getY() - rand.nextInt(500) - 100;

		Squidoe squid = new Squidoe(randomX, randomY);
		level.addMobile(squid);

		squidAI.execute(new Runnable() {

			@Override
			public void run() {

				while (true)
					squid.AI(level);
			}
		});
	}

}
