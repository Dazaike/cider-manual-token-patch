.class public final Lcom/cidercollective/cider/auth/manual/ManualAppleTokenActivity$NegativeClickListener;
.super Ljava/lang/Object;
.implements Landroid/content/DialogInterface$OnClickListener;


# instance fields
.field private final activity:Landroid/app/Activity;


# direct methods
.method public constructor <init>(Landroid/app/Activity;)V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    iput-object p1, p0, Lcom/cidercollective/cider/auth/manual/ManualAppleTokenActivity$NegativeClickListener;->activity:Landroid/app/Activity;

    return-void
.end method


# virtual methods
.method public final onClick(Landroid/content/DialogInterface;I)V
    .locals 1

    iget-object v0, p0, Lcom/cidercollective/cider/auth/manual/ManualAppleTokenActivity$NegativeClickListener;->activity:Landroid/app/Activity;

    invoke-virtual {v0}, Landroid/app/Activity;->finish()V

    return-void
.end method
