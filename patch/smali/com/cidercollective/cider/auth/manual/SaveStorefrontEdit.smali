.class public final Lcom/cidercollective/cider/auth/manual/SaveStorefrontEdit;
.super Ljava/lang/Object;
.implements Lc/zh2;


# instance fields
.field private final encryptedValue:Ljava/lang/String;


# direct methods
.method public constructor <init>(Ljava/lang/String;)V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    iput-object p1, p0, Lcom/cidercollective/cider/auth/manual/SaveStorefrontEdit;->encryptedValue:Ljava/lang/String;

    return-void
.end method


# virtual methods
.method public final m(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    .locals 2

    check-cast p1, Lc/kk4;

    sget-object v0, Lc/aa6;->b:Lc/fi5;

    iget-object v1, p0, Lcom/cidercollective/cider/auth/manual/SaveStorefrontEdit;->encryptedValue:Ljava/lang/String;

    invoke-virtual {p1, v0, v1}, Lc/kk4;->e(Lc/fi5;Ljava/lang/Object;)V

    sget-object v0, Lc/pl7;->a:Lc/pl7;

    return-object v0
.end method
