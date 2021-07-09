.class public Main
.super java/lang/Object

.method public <init>()V
.limit stack 32
.limit locals 32
0: aload 0
1: invokespecial java/lang/Object/<init>()V
4: return
.end method

.method public static main([Ljava/lang/String;)V
.limit stack 32
.limit locals 32
new Fibonacci
dup
ldc 2
invokespecial Fibonacci/<init>(I)V
astore 1
new Square
dup
ldc 2
invokespecial Square/<init>(I)V
astore 2
aload 1
aload 2
invokevirtual Fibonacci/setKnownActors(LSquare;)V
aload 2
aload 1
invokevirtual Square/setKnownActors(LFibonacci;)V
aload 1
invokevirtual Fibonacci/initial()V
aload 2
invokevirtual Square/initial()V

aload 1
invokevirtual Fibonacci/start()V
aload 2
invokevirtual Square/start()V
return
.end method
