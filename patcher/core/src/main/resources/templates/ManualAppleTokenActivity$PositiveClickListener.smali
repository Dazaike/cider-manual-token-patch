.class public final Lcom/cidercollective/cider/auth/manual/ManualAppleTokenActivity$PositiveClickListener;
.super Ljava/lang/Object;
.implements Landroid/view/View$OnClickListener;


# instance fields
.field private final activity:Landroid/app/Activity;

.field private final dialog:Landroid/content/DialogInterface;

.field private final editText:Landroid/widget/EditText;


# direct methods
.method public constructor <init>(Landroid/app/Activity;Landroid/widget/EditText;Landroid/content/DialogInterface;)V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    iput-object p1, p0, Lcom/cidercollective/cider/auth/manual/ManualAppleTokenActivity$PositiveClickListener;->activity:Landroid/app/Activity;

    iput-object p2, p0, Lcom/cidercollective/cider/auth/manual/ManualAppleTokenActivity$PositiveClickListener;->editText:Landroid/widget/EditText;

    iput-object p3, p0, Lcom/cidercollective/cider/auth/manual/ManualAppleTokenActivity$PositiveClickListener;->dialog:Landroid/content/DialogInterface;

    return-void
.end method


# virtual methods
.method public final onClick(Landroid/view/View;)V
    .locals 4

    iget-object v0, p0, Lcom/cidercollective/cider/auth/manual/ManualAppleTokenActivity$PositiveClickListener;->editText:Landroid/widget/EditText;

    invoke-virtual {v0}, Landroid/widget/EditText;->getText()Landroid/text/Editable;

    move-result-object v0

    invoke-virtual {v0}, Ljava/lang/Object;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-virtual {v0}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v0

    invoke-virtual {v0}, Ljava/lang/String;->isEmpty()Z

    move-result v1

    if-eqz v1, :cond_has_text

    iget-object v1, p0, Lcom/cidercollective/cider/auth/manual/ManualAppleTokenActivity$PositiveClickListener;->activity:Landroid/app/Activity;

    const-string v2, "Enter a token"

    const/4 v3, 0x0

    invoke-static {v1, v2, v3}, Landroid/widget/Toast;->makeText(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;

    move-result-object v1

    invoke-virtual {v1}, Landroid/widget/Toast;->show()V

    return-void

    :cond_has_text
    iget-object v1, p0, Lcom/cidercollective/cider/auth/manual/ManualAppleTokenActivity$PositiveClickListener;->dialog:Landroid/content/DialogInterface;

    invoke-interface {v1}, Landroid/content/DialogInterface;->dismiss()V

    iget-object v1, p0, Lcom/cidercollective/cider/auth/manual/ManualAppleTokenActivity$PositiveClickListener;->activity:Landroid/app/Activity;

    new-instance v2, Lcom/cidercollective/cider/auth/manual/ManualAppleTokenActivity$SignInThread;

    invoke-direct {v2, v1, v0}, Lcom/cidercollective/cider/auth/manual/ManualAppleTokenActivity$SignInThread;-><init>(Landroid/app/Activity;Ljava/lang/String;)V

    invoke-virtual {v2}, Ljava/lang/Thread;->start()V

    return-void
.end method
