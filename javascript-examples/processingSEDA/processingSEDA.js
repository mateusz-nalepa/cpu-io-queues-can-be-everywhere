import MutexLimiter from "./mutexLimiter.js";
import {logMessage} from "../common/logMessage.js";

const mutexLimiter = new MutexLimiter()

const yieldToEventLoop = () => new Promise((resolve) => setTimeout(resolve, 0));

const delaySync = (ms) => {
    const end = Date.now() + ms;
    while (Date.now() < end) {
    } // busy wait
};

async function smallProcessingStep(taskNumber, label, results) {
    logMessage("### Task number: " + taskNumber + " Processing step: " + label)
    results.push(label);
}


export async function limitedBigProcessingChunked(taskNumber) {
    const startQueueWaitTime = Date.now();

    await mutexLimiter.execute(async () => {
        const elapsed = Date.now() - startQueueWaitTime;

        logMessage(`### Task number: ${taskNumber} waited ${elapsed} ms in the queue.`)

        await internalBigProcessingChunked(taskNumber);
    });
}

export async function internalBigProcessingChunked(taskNumber) {
    logMessage("### Start big function for task number: " + taskNumber)
    const startBigProcessingChunkedTime = Date.now();

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
        delaySync(1000)
        await yieldToEventLoop(); // eventLoop has time to do something
    }
    const elapsed = Date.now() - startBigProcessingChunkedTime;

    logMessage("### Ended big function for task number: " + taskNumber + " after: " + elapsed + " ms")
    return results;
}
