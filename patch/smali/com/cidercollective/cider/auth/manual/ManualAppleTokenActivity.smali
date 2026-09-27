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

    sget-object v0, Lc/w32;->n:Lc/w32;

    invoke-virtual {v0, p0}, Lc/w32;->w(Landroid/content/ContextWrapper;)V

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
