.class public Lcom/cidercollective/cider/auth/manual/ManualAppleTokenActivity;
.super Landroid/app/Activity;
.source "ManualAppleTokenActivity.java"


# direct methods
.method public constructor <init>()V
    .locals 0

    invoke-direct {p0}, Landroid/app/Activity;-><init>()V

    return-void
.end method

.method public onBackPressed()V
    .locals 0

    return-void
.end method

.method protected onCreate(Landroid/os/Bundle;)V
    .locals 8

    invoke-super {p0, p1}, Landroid/app/Activity;->onCreate(Landroid/os/Bundle;)V

    sget-object v0, {{F_CIDER_RUNTIME_INSTANCE}}

    invoke-virtual {v0, p0}, {{M_ENSURE_INITIALIZED}}

    # BEGIN token-extra
    invoke-virtual {p0}, Landroid/app/Activity;->getIntent()Landroid/content/Intent;

    move-result-object v1

    const-string v2, "music_user_token"

    invoke-virtual {v1, v2}, Landroid/content/Intent;->getStringExtra(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v1

    if-eqz v1, :no_token_extra

    invoke-virtual {v1}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/String;->isEmpty()Z

    move-result v2

    if-nez v2, :no_token_extra

    new-instance v2, Lcom/cidercollective/cider/auth/manual/ManualAppleTokenActivity$SignInThread;

    invoke-direct {v2, p0, v1}, Lcom/cidercollective/cider/auth/manual/ManualAppleTokenActivity$SignInThread;-><init>(Landroid/app/Activity;Ljava/lang/String;)V

    invoke-virtual {v2}, Ljava/lang/Thread;->start()V

    return-void

    :no_token_extra
    # END token-extra

    new-instance v1, Landroid/widget/EditText;

    invoke-direct {v1, p0}, Landroid/widget/EditText;-><init>(Landroid/content/Context;)V

    const-string v2, "media-user-token"

    invoke-virtual {v1, v2}, Landroid/widget/EditText;->setHint(Ljava/lang/CharSequence;)V

    new-instance v3, Landroid/app/AlertDialog$Builder;

    invoke-direct {v3, p0}, Landroid/app/AlertDialog$Builder;-><init>(Landroid/content/Context;)V

    const-string v4, "Sign in with Music-User-Token"

    invoke-virtual {v3, v4}, Landroid/app/AlertDialog$Builder;->setTitle(Ljava/lang/CharSequence;)Landroid/app/AlertDialog$Builder;

    const-string v4, "Paste the value of the media-user-token cookie/localStorage entry from an authenticated music.apple.com session (e.g. the one Cider desktop already uses)."

    invoke-virtual {v3, v4}, Landroid/app/AlertDialog$Builder;->setMessage(Ljava/lang/CharSequence;)Landroid/app/AlertDialog$Builder;

    invoke-virtual {v3, v1}, Landroid/app/AlertDialog$Builder;->setView(Landroid/view/View;)Landroid/app/AlertDialog$Builder;

    const/4 v4, 0x0

    invoke-virtual {v3, v4}, Landroid/app/AlertDialog$Builder;->setCancelable(Z)Landroid/app/AlertDialog$Builder;

    new-instance v5, Lcom/cidercollective/cider/auth/manual/ManualAppleTokenActivity$NegativeClickListener;

    invoke-direct {v5, p0}, Lcom/cidercollective/cider/auth/manual/ManualAppleTokenActivity$NegativeClickListener;-><init>(Landroid/app/Activity;)V

    const-string v6, "Cancel"

    invoke-virtual {v3, v6, v5}, Landroid/app/AlertDialog$Builder;->setNegativeButton(Ljava/lang/CharSequence;Landroid/content/DialogInterface$OnClickListener;)Landroid/app/AlertDialog$Builder;

    const-string v6, "Sign in"

    const/4 v7, 0x0

    invoke-virtual {v3, v6, v7}, Landroid/app/AlertDialog$Builder;->setPositiveButton(Ljava/lang/CharSequence;Landroid/content/DialogInterface$OnClickListener;)Landroid/app/AlertDialog$Builder;

    invoke-virtual {v3}, Landroid/app/AlertDialog$Builder;->create()Landroid/app/AlertDialog;

    move-result-object v3

    new-instance v6, Lcom/cidercollective/cider/auth/manual/ManualAppleTokenActivity$OnShowListener;

    invoke-direct {v6, p0, v1}, Lcom/cidercollective/cider/auth/manual/ManualAppleTokenActivity$OnShowListener;-><init>(Landroid/app/Activity;Landroid/widget/EditText;)V

    invoke-virtual {v3, v6}, Landroid/app/AlertDialog;->setOnShowListener(Landroid/content/DialogInterface$OnShowListener;)V

    invoke-virtual {v3}, Landroid/app/AlertDialog;->show()V

    return-void
.end method
