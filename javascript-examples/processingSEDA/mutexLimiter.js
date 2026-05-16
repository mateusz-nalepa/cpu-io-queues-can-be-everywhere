import pLimit from "p-limit";

class MutexLimiter {
    #limit;

    constructor() {
        this.#limit = pLimit(1);
    }

    async execute(fn) {
        return this.#limit(fn);
    }
}

export default MutexLimiter;