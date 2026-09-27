.class public final Lcom/cidercollective/cider/auth/manual/ManualAppleTokenActivity$OnShowListener;
.super Ljava/lang/Object;
.implements Landroid/content/DialogInterface$OnShowListener;


# instance fields
.field private final activity:Landroid/app/Activity;

.field private final editText:Landroid/widget/EditText;


# direct methods
.method public constructor <init>(Landroid/app/Activity;Landroid/widget/EditText;)V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    iput-object p1, p0, Lcom/cidercollective/cider/auth/manual/ManualAppleTokenActivity$OnShowListener;->activity:Landroid/app/Activity;

    iput-object p2, p0, Lcom/cidercollective/cider/auth/manual/ManualAppleTokenActivity$OnShowListener;->editText:Landroid/widget/EditText;

    return-void
.end method


# virtual methods
.method public final onShow(Landroid/content/DialogInterface;)V
    .locals 4

    check-cast p1, Landroid/app/AlertDialog;

    const/4 v0, -0x1

    invoke-virtual {p1, v0}, Landroid/app/AlertDialog;->getButton(I)Landroid/widget/Button;

    move-result-object v1

    iget-object v2, p0, Lcom/cidercollective/cider/auth/manual/ManualAppleTokenActivity$OnShowListener;->activity:Landroid/app/Activity;

    iget-object v3, p0, Lcom/cidercollective/cider/auth/manual/ManualAppleTokenActivity$OnShowListener;->editText:Landroid/widget/EditText;

    new-instance v0, Lcom/cidercollective/cider/auth/manual/ManualAppleTokenActivity$PositiveClickListener;

    invoke-direct {v0, v2, v3, p1}, Lcom/cidercollective/cider/auth/manual/ManualAppleTokenActivity$PositiveClickListener;-><init>(Landroid/app/Activity;Landroid/widget/EditText;Landroid/content/DialogInterface;)V

    invoke-virtual {v1, v0}, Landroid/widget/Button;->setOnClickListener(Landroid/view/View$OnClickListener;)V

    return-void
.end method
