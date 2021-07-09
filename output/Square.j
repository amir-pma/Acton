.class public Square
.super Actor

.field fibonacci LFibonacci;
.field name Ljava/lang/String;
.field number [I
.field slot I

.method public <init>(I)V
.limit stack 32
.limit locals 32
aload 0
ldc ""
putfield Square/name Ljava/lang/String;
aload 0
ldc 5
newarray int
putfield Square/number [I
aload 0
ldc 0
putfield Square/slot I
aload 0
iload 1
invokespecial Actor/<init>(I)V
return
.end method

.method public setKnownActors(LFibonacci;)V
.limit stack 32
.limit locals 32
aload 0
aload 1
putfield Square/fibonacci LFibonacci;

return
.end method

.method public initial()V
.limit stack 32
.limit locals 32
aload 0
ldc "Square"
putfield Square/name Ljava/lang/String;
return
.end method


.method public evaluate(LActor;I)V
.limit stack 32
.limit locals 32
ldc 0
istore 3
aload 0
getfield Square/slot I
ldc 5
if_icmpge Label_26
ldc 1
goto Label_27
Label_26:
ldc 0
Label_27:
ifeq Label_24
ldc 0
istore 3
Label_28:
iload 3
iload 2
if_icmpge Label_31
ldc 1
goto Label_32
Label_31:
ldc 0
Label_32:
ifeq Label_29
iload 2
iload 3
iload 3
imul
if_icmpne Label_35
ldc 1
goto Label_36
Label_35:
ldc 0
Label_36:
ifeq Label_33
aload 0
getfield Square/number [I
aload 0
getfield Square/slot I
iload 2
iastore
aload 0
aload 0
getfield Square/slot I
ldc 1
iadd
putfield Square/slot I
getstatic java/lang/System/out Ljava/io/PrintStream;
aload 0
getfield Square/name Ljava/lang/String;
invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V
getstatic java/lang/System/out Ljava/io/PrintStream;
ldc "slots full:"
invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V
getstatic java/lang/System/out Ljava/io/PrintStream;
aload 0
getfield Square/slot I
invokevirtual java/io/PrintStream/println(I)V
getstatic java/lang/System/out Ljava/io/PrintStream;
ldc " "
invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V
goto Label_34
Label_33:
Label_34:
Label_30:
iload 3
ldc 1
iadd
istore 3
goto Label_28
Label_29:
goto Label_25
Label_24:
Label_25:
aload 0
getfield Square/slot I
ldc 5
if_icmpne Label_39
ldc 1
goto Label_40
Label_39:
ldc 0
Label_40:
ifeq Label_37
getstatic java/lang/System/out Ljava/io/PrintStream;
aload 0
getfield Square/number [I
invokestatic java/util/Arrays/toString([I)Ljava/lang/String;
invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V
getstatic java/lang/System/out Ljava/io/PrintStream;
ldc " "
invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V
aload 0
aload 0
getfield Square/slot I
ldc 1
iadd
putfield Square/slot I
goto Label_38
Label_37:
Label_38:
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
new Square_evaluate
dup
aload 0
aload 1
iload 2
invokespecial Square_evaluate/<init>(LSquare;LActor;I)V
invokevirtual Square/send(LMessage;)V
return
.end method

