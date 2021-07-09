.class public Square_evaluate
.super Message

.field private counter I
.field private receiver LSquare;
.field private sender LActor;

.method public <init>(LSquare;LActor;I)V
.limit stack 32
.limit locals 32
aload 0
invokespecial Message/<init>()V

aload 0
ldc 0
putfield Square_evaluate/counter I
aload 0
aload 1
putfield Square_evaluate/receiver LSquare;
aload 0
aload 2
putfield Square_evaluate/sender LActor;
aload 0
iload 3
putfield Square_evaluate/counter I
return
.end method

.method public execute()V
.limit stack 32
.limit locals 32
aload 0
getfield Square_evaluate/receiver LSquare;
aload 0
getfield Square_evaluate/sender LActor;
aload 0
getfield Square_evaluate/counter I
invokevirtual Square/evaluate(LActor;I)V
return
.end method
