package com.github.rccookie.http;

import com.github.rccookie.util.Console;

@FunctionalInterface
interface Computation<T> {

    default T compute() throws Exception {
        Console.log("Computing...");
        return compute0();
    }
    T compute0() throws Exception;
}
