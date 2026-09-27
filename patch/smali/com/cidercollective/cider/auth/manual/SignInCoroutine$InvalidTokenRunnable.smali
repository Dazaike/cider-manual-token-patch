.class public final Lcom/cidercollective/cider/auth/manual/SignInCoroutine$InvalidTokenRunnable;
.super Ljava/lang/Object;
.implements Ljava/lang/Runnable;


# instance fields
.field private final activity:Landroid/app/Activity;


# direct methods
.method public constructor <init>(Landroid/app/Activity;)V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    iput-object p1, p0, Lcom/cidercollective/cider/auth/manual/SignInCoroutine$InvalidTokenRunnable;->activity:Landroid/app/Activity;

    return-void
.end method


# virtual methods
.method public final run()V
    .locals 3

    iget-object v0, p0, Lcom/cidercollective/cider/auth/manual/SignInCoroutine$InvalidTokenRunnable;->activity:Landroid/app/Activity;

    const-string v1, "That token isn\'t valid or has expired."

    const/4 v2, 0x0

    invoke-static {v0, v1, v2}, Landroid/widget/Toast;->makeText(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;

    move-result-object v1

    invoke-virtual {v1}, Landroid/widget/Toast;->show()V

    invoke-virtual {v0}, Landroid/app/Activity;->finish()V

    return-void
.end method
