package com.templeosrs.util.api;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class QuadraticBackoffStrategy
{
	@Getter
	@Setter
	private boolean submitting = false;

	private int requestAttemptCount = 0;
	private int cycleCount = 0;

	public void reset()
	{
		submitting = false;
		requestAttemptCount = 0;
		cycleCount = 0;
	}

	public void beginNewRequestAttempt()
	{
		requestAttemptCount++;
	}

	public void finishCycle()
	{
		submitting = false;
	}

	public boolean shouldSkipRequest()
	{
		cycleCount++;

		final boolean shouldSkip = Math.pow((int) Math.sqrt(cycleCount), 2) != cycleCount;

		if (shouldSkip)
		{
			setSubmitting(false);

			log.debug("⚠️ Skipping request due to backoff configuration");
		}
		else
		{
			beginNewRequestAttempt();
		}

		return shouldSkip;
	}

	public boolean isRequestLimitReached()
	{
		int maxRetries = 3;
		final boolean isRequestLimitReached = requestAttemptCount >= maxRetries;

		if (isRequestLimitReached)
		{
			log.error("❌ Maximum number of retries reached; aborting request!");

			reset();
		}

		return isRequestLimitReached;
	}
}
