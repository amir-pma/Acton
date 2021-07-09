.class public Fibonacci
.super Actor

.field square LSquare;
.field name Ljava/lang/String;
.field number [I
.field slot I

.method public <init>(I)V
.limit stack 32
.limit locals 32
aload 0
ldc ""
putfield Fibonacci/name Ljava/lang/String;
aload 0
ldc 5
newarray int
putfield Fibonacci/number [I
aload 0
ldc 0
putfield Fibonacci/slot I
aload 0
iload 1
invokespecial Actor/<init>(I)V
return
.end method

.method public setKnownActors(LSquare;)V
.limit stack 32
.limit locals 32
aload 0
aload 1
putfield Fibonacci/square LSquare;

return
.end method

.method public initial()V
.limit stack 32
.limit locals 32
aload 0
ldc "Fibo"
putfield Fibonacci/name Ljava/lang/String;
aload 0
getfield Fibonacci/square LSquare;
aload 0
ldc 5
invokevirtual Square/send_evaluate(LActor;I)V
return
.end method


.method public evaluate(LActor;I)V
.limit stack 32
.limit locals 32
ldc 0
istore 3
ldc 0
istore 4
ldc 0
istore 5
aload 0
getfield Fibonacci/slot I
ldc 5
if_icmpge Label_5
ldc 1
goto Label_6
Label_5:
ldc 0
Label_6:
ifeq Label_3
aload 0
getfield Fibonacci/number [I
aload 0
getfield Fibonacci/slot I
iaload
ldc 1
ineg
if_icmpeq Label_7
ldc 1
goto Label_8
Label_7:
ldc 0
Label_8:
ifeq Label_3
ldc 1
goto Label_4
Label_3:
ldc 0
Label_4:
ifeq Label_1
ldc 1
istore 3
ldc 1
istore 4
iload 3
iload 4
iadd
istore 5
Label_9:
iload 5
iload 2
if_icmpge Label_14
ldc 1
goto Label_15
Label_14:
ldc 0
Label_15:
ifeq Label_12
iload 4
istore 3
iload 5
istore 4
iload 3
iload 4
iadd
istore 5
goto Label_11
goto Label_13
Label_12:
Label_13:
iload 5
iload 2
if_icmpne Label_18
ldc 1
goto Label_19
Label_18:
ldc 0
Label_19:
ifeq Label_16
aload 0
getfield Fibonacci/number [I
aload 0
getfield Fibonacci/slot I
iload 2
iastore
getstatic java/lang/System/out Ljava/io/PrintStream;
aload 0
getfield Fibonacci/name Ljava/lang/String;
invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V
getstatic java/lang/System/out Ljava/io/PrintStream;
ldc "slots remaining:"
invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V
getstatic java/lang/System/out Ljava/io/PrintStream;
ldc 5
aload 0
dup
getfield Fibonacci/slot I
dup_x1
ldc 1
iadd
putfield Fibonacci/slot I
isub
invokevirtual java/io/PrintStream/println(I)V
getstatic java/lang/System/out Ljava/io/PrintStream;
ldc " "
invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V
goto Label_17
Label_16:
Label_17:
goto Label_10
Label_11:
goto Label_9
Label_10:
goto Label_2
Label_1:
aload 0
getfield Fibonacci/slot I
ldc 5
if_icmpne Label_22
ldc 1
goto Label_23
Label_22:
ldc 0
Label_23:
ifeq Label_20
getstatic java/lang/System/out Ljava/io/PrintStream;
aload 0
getfield Fibonacci/number [I
invokestatic java/util/Arrays/toString([I)Ljava/lang/String;
invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V
getstatic java/lang/System/out Ljava/io/PrintStream;
ldc " "
invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V
aload 0
aload 0
getfield Fibonacci/slot I
ldc 1
iadd
putfield Fibonacci/slot I
goto Label_21
Label_20:
Label_21:
Label_2:
aload 1
aload 0
iload 2
ldc 1
iadd
invokevirtual Actor/send_evaluate(LActor;I)V
return
.end method

.method public send_evaluate(LActor;I)V
.limit stack 32
.limit locals 32
aload 0
new Fibonacci_evaluate
dup
aload 0
aload 1
iload 2
invokespecial Fibonacci_evaluate/<init>(LFibonacci;LActor;I)V
invokevirtual Fibonacci/send(LMessage;)V
return
.end method

