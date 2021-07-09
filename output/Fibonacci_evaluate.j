.class public Fibonacci_evaluate
.super Message

.field private counter I
.field private receiver LFibonacci;
.field private sender LActor;

.method public <init>(LFibonacci;LActor;I)V
.limit stack 32
.limit locals 32
aload 0
invokespecial Message/<init>()V

aload 0
ldc 0
putfield Fibonacci_evaluate/counter I
aload 0
aload 1
putfield Fibonacci_evaluate/receiver LFibonacci;
aload 0
aload 2
putfield Fibonacci_evaluate/sender LActor;
aload 0
iload 3
putfield Fibonacci_evaluate/counter I
return
.end method

.method public execute()V
.limit stack 32
.limit locals 32
aload 0
getfield Fibonacci_evaluate/receiver LFibonacci;
aload 0
getfield Fibonacci_evaluate/sender LActor;
aload 0
getfield Fibonacci_evaluate/counter I
invokevirtual Fibonacci/evaluate(LActor;I)V
return
.end method
