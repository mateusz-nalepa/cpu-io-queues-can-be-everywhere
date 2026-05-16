import {bigProcessing} from "./processingDefault/processingDefault.js";
import {limitedBigProcessing} from "./processingSEDA/processingSEDA.js";

console.log("############### Default");
// this will block event loop for a long time
// tasks are being executed synchronously
await Promise.all([
    bigProcessing(1),
    bigProcessing(2)
]);

console.log();
console.log();
console.log();
console.log();
console.log();
console.log("############### SEDA");

// this won't block event loop
// tasks are being executed asynchronously
// but thanks to mutex, from logical point of view,
// they are still executed one after another,
await Promise.all([
    limitedBigProcessing(1),
    limitedBigProcessing(2)
]);
