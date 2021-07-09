.class public DefaultActor
.super java/lang/Thread

.method public <init>()V
.limit stack 32
.limit locals 32
aload 0
invokespecial java/lang/Thread/<init>()V
return
.end method

.method public send_evaluate(LActor;I)V
.limit stack 32
.limit locals 32
getstatic java/lang/System/out Ljava/io/PrintStream;
ldc "there is no msghandler named evaluate in sender"
invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V
return
.end method

