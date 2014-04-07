package kan.illuminated.chords.net;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * async job that do some retries in hope to accomplish the task
 *
 * @author KAN
 */
public abstract class PersistentTask<Input, Output> {

	/**
	 * task will be rescheduled if it throws this exception
	 */
	public static class PersistentTaskException extends RuntimeException {
		public PersistentTaskException() {
		}

		public PersistentTaskException(Throwable throwable) {
			super(throwable);
		}
	}

	private class TaskRunnable implements Runnable {

		private Input in;

		private int tryCount;

		private TaskRunnable(Input in, int tryCount) {
			this.in = in;
			this.tryCount = tryCount;
		}

		@Override
		public void run() {

			Output out = null;
			boolean failed = false;
			try {
				out = PersistentTask.this.run(in);
			} catch (PersistentTaskException e) {
				// TODO - log
				e.printStackTrace();

				if (tryCount < tries.length) {
					if (!executingFuture.isCancelled()) {
						executeTask(new TaskRunnable(in, tryCount + 1), tries[tryCount + 1], TimeUnit.SECONDS);
					}
				} else {
					failed = true;
				}
			} catch (Throwable e) {
				e.printStackTrace();

				failed = true;
			}

			if (out != null) {
				try {
					onResponseTaskThread(out);
				} catch (Throwable e) {
					e.printStackTrace();
				}

				if (handler != null) {
					final Output callerOut = out;
					handler.post(new Runnable() {
						@Override
						public void run() {
							try {
								onResponseCallerThread(callerOut);
							} catch (Throwable e) {
								e.printStackTrace();
							}
						}
					});
				}

			}

			if (failed) {
				try {
					onFailTaskThread();
				} catch (Throwable e) {
					e.printStackTrace();
				}

				if (handler != null) {
					handler.post(new Runnable() {
						@Override
						public void run() {
							try {
								onFailCallerThread();
							} catch (Throwable e) {
								e.printStackTrace();
							}
						}
					});
				}
			}
		}
	}

	private static final ScheduledExecutorService requestExecutor = Executors.newScheduledThreadPool(5);

	/**
	 * retry count with delays in seconds before each try
	 */
	private static final int[] tries = {0, 1, 3};

	private Future<?> executingFuture;


	private Handler handler;

	private Input in;

	protected PersistentTask() {

		if (Looper.myLooper() != null) {
			handler = new Handler();
		}
	}

	public Input getInput() {
		return in;
	}

	public void execute(Input in) {

		if (executingFuture != null) {
			throw new RuntimeException("same task can't be executed more than once");
		}

		this.in = in;

		executeTask(new TaskRunnable(in, 1));
	}

	private void executeTask(TaskRunnable task) {
		executeTask(task, 0, TimeUnit.SECONDS);
	}

	private void executeTask(TaskRunnable task, long delay, TimeUnit unit) {

		if (executingFuture != null && executingFuture.isCancelled()) {
			// this task is already cancelled
			return;
		}

		Future<?> f = executingFuture;

		executingFuture = requestExecutor.schedule(task, delay, unit);

		if (f != null && f.isCancelled()) {
			// task just got cancelled - rollback last schedule
			executingFuture.cancel(true);
		}
	}

	public void cancel() {
		if (executingFuture != null) {
			executingFuture.cancel(true);
		}
	}

	public boolean isNew() {
		return executingFuture == null;
	}

	public boolean isRunning() {
		return executingFuture != null &&
				(!executingFuture.isCancelled() && !executingFuture.isDone());
	}

	public boolean isCompleted() {
		return executingFuture != null &&
				(executingFuture.isDone() || executingFuture.isCancelled());
	}

	protected abstract Output run(Input in);

	protected void onResponseTaskThread(Output out) {
	}

	/**
	 * called only if caller thread is ui thread
	 */
	protected void onResponseCallerThread(Output out) {
	}

	protected void onFailTaskThread() {
	}

	/**
	 * called only if caller thread is ui thread
	 */
	protected void onFailCallerThread() {
	}
}
