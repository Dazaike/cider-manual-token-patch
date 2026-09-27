.class public final Lcom/cidercollective/cider/auth/manual/ManualAppleTokenActivity$SignInThread;
.super Ljava/lang/Thread;


# instance fields
.field private final activity:Landroid/app/Activity;

.field private final token:Ljava/lang/String;


# direct methods
.method public constructor <init>(Landroid/app/Activity;Ljava/lang/String;)V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Thread;-><init>()V

    iput-object p1, p0, Lcom/cidercollective/cider/auth/manual/ManualAppleTokenActivity$SignInThread;->activity:Landroid/app/Activity;

    iput-object p2, p0, Lcom/cidercollective/cider/auth/manual/ManualAppleTokenActivity$SignInThread;->token:Ljava/lang/String;

    return-void
.end method


# virtual methods
.method public final run()V
    .locals 4

    iget-object v0, p0, Lcom/cidercollective/cider/auth/manual/ManualAppleTokenActivity$SignInThread;->activity:Landroid/app/Activity;

    iget-object v1, p0, Lcom/cidercollective/cider/auth/manual/ManualAppleTokenActivity$SignInThread;->token:Ljava/lang/String;

    const/4 v2, 0x0

    new-instance v3, Lcom/cidercollective/cider/auth/manual/SignInCoroutine;

    invoke-direct {v3, v0, v1, v2}, Lcom/cidercollective/cider/auth/manual/SignInCoroutine;-><init>(Landroid/app/Activity;Ljava/lang/String;Lc/qb1;)V

    invoke-static {v3}, Lc/pe8;->h(Lc/zh2;)Ljava/lang/Object;

    return-void
.end method
