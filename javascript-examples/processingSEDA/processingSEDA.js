import MutexLimiter from "./mutexLimiter.js";

const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

const mutexLimiter = new MutexLimiter()

async function smallProcessingStep(taskNumber, label, results) {
    logMessage("### Task number: " + taskNumber + " Processing step: " + label)
    results.push(label);
}


export async function limitedBigProcessing(taskNumber) {
    const startQueueWaitTime = Date.now();

    await mutexLimiter.execute(async () => {
        const elapsed = Date.now() - startQueueWaitTime;
        console.log(`Task number: ${taskNumber} waited ${elapsed} ms in the queue.`);

        await internalBigProcessingChunked(taskNumber);
    });
}

export async function internalBigProcessingChunked(taskNumber) {
    logMessage("### Start big function for task number: " + taskNumber)
    const results = [];

    const steps = [
        () => smallProcessingStep(taskNumber, "step 1", results),
        () => smallProcessingStep(taskNumber, "step 2", results),
        () => smallProcessingStep(taskNumber, "step 3", results),
        () => smallProcessingStep(taskNumber, "step 4", results),
        () => smallProcessingStep(taskNumber, "step 5", results),
    ];

    for (const step of steps) {
        await step();
        await delay(1000); // eventLoop has time to do something
    }

    logMessage("### Ended big function for task number: " + taskNumber)
    return results;
}


function logMessage(message) {
    const d = new Date();
    console.log(`${d.getHours()}:${d.getMinutes()}:${d.getSeconds()}.${d.getMilliseconds()}   ` + message);
}