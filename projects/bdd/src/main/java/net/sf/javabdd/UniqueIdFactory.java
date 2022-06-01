package net.sf.javabdd;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import java.math.BigInteger;
import java.util.BitSet;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** A {@link BDDFactory}. */
public final class UniqueIdFactory extends BDDFactory {
  /**
   * A 128-bit identifier for a BDD, which is a good (birthday-paradox-strong) hash of its level,
   * left Id, and right Id. Because the hash is birthday paradox strong, it provides with probably >
   * 1-10^-9 that if Id are equal, they correspond to the same BDD.
   */
  private static final class Id {
    final long _hi;
    final long _lo;

    public Id(long hi, long lo) {
      _hi = hi;
      _lo = lo;
    }

    @Override
    public int hashCode() {
      return (int) _lo;
    }

    @Override
    public boolean equals(Object other) {
      if (other == this) {
        return true;
      } else if (!(other instanceof Id)) {
        return false;
      }
      Id o = (Id) other;
      return _hi == o._hi && _lo == o._lo;
    }
  }

  private static Id id(int level, BddRepr lo, BddRepr hi) {
    return idImpl(level, lo._id._hi, lo._id._lo, hi._id._hi, hi._id._lo);
  }

  private static Id idImpl(int level, long loHi, long loLo, long hiHi, long hiLo) {
    // Todo: replace this with a custom impl like xxh3_128.
    HashCode hc =
        Hashing.murmur3_128()
            .newHasher(36)
            .putInt(level)
            .putLong(loHi)
            .putLong(loLo)
            .putLong(hiHi)
            .putLong(hiLo)
            .hash();
    assert hc.bits() == 128;
    byte[] bytes = hc.asBytes();
    long idHi =
        ((bytes[0] & 0xFFL) << 7)
            + ((bytes[1] & 0xFFL) << 6)
            + ((bytes[2] & 0xFFL) << 5)
            + ((bytes[3] & 0xFFL) << 4)
            + ((bytes[4] & 0xFFL) << 3)
            + ((bytes[5] & 0xFFL) << 2)
            + ((bytes[6] & 0xFFL) << 1)
            + ((bytes[7] & 0xFFL));
    long idLo =
        ((bytes[8] & 0xFFL) << 7)
            + ((bytes[9] & 0xFFL) << 6)
            + ((bytes[10] & 0xFFL) << 5)
            + ((bytes[11] & 0xFFL) << 4)
            + ((bytes[12] & 0xFFL) << 3)
            + ((bytes[13] & 0xFFL) << 2)
            + ((bytes[14] & 0xFFL) << 1)
            + ((bytes[15] & 0xFFL));
    return new Id(idHi, idLo);
  }

  private static final Id ZERO_ID = idImpl(-1, 0, 0, 0, 0);
  private static final Id ONE_ID = idImpl(-1, 1, 1, 1, 1);

  /** The internal representation a BDD. */
  private static final class BddRepr {
    final Id _id;
    final int _level;
    final BddRepr _lo;
    final BddRepr _hi;
    final AtomicInteger _refCount;

    private BddRepr(Id id, int level, BddRepr lo, BddRepr hi) {
      assert (lo == null) == (hi == null) && (lo == null) == (id == ZERO_ID || id == ONE_ID);
      _level = level;
      _lo = lo;
      _hi = hi;
      _id = id;
      _refCount = new AtomicInteger();
    }

    void addRef() {
      _refCount.incrementAndGet();
    }
  }

  public UniqueIdFactory() {
    _idToRepr = new ConcurrentHashMap<>();
    _zero = new BddRepr(ZERO_ID, -1, null, null);
    _one = new BddRepr(ONE_ID, -1, null, null);
  }

  private final ConcurrentHashMap<Id, BddRepr> _idToRepr;
  private final BddRepr _zero;
  private final BddRepr _one;

  BddRepr makeBdd(int level, @Nonnull BddRepr lo, @Nonnull BddRepr hi) {
    assert level >= 0;
    if (lo == hi) {
      lo.addRef();
      return lo;
    }
    Id id = id(level, lo, hi);
    BddRepr ret = _idToRepr.computeIfAbsent(id, i -> new BddRepr(i, level, lo, hi));
    ret.addRef();
    lo.addRef();
    hi.addRef();
    return ret;
  }

  /////
  private class UserBdd extends BDD {
    private BddRepr _repr;

    private UserBdd(BddRepr repr) {
      _repr = repr;
    }

    @Override
    public BDDFactory getFactory() {
      return UniqueIdFactory.this;
    }

    @Override
    public boolean isAssignment() {
      return false;
    }

    @Override
    public boolean isZero() {
      return false;
    }

    @Override
    public boolean isOne() {
      return false;
    }

    @Override
    public int var() {
      return 0;
    }

    @Override
    public BDD high() {
      return null;
    }

    @Override
    public BDD low() {
      return null;
    }

    @Override
    public BDD id() {
      return null;
    }

    @Override
    public BDD not() {
      return null;
    }

    @Override
    public boolean andSat(BDD that) {
      return false;
    }

    @Override
    public boolean diffSat(BDD that) {
      return false;
    }

    @Override
    public BDD ite(BDD thenBDD, BDD elseBDD) {
      return null;
    }

    @Override
    public BDD relprod(BDD that, BDD var) {
      return null;
    }

    @Override
    public BDD compose(BDD g, int var) {
      return null;
    }

    @Override
    public BDD veccompose(BDDPairing pair) {
      return null;
    }

    @Override
    public BDD constrain(BDD that) {
      return null;
    }

    @Override
    BDD exist(BDD var, boolean makeNew) {
      return null;
    }

    @Override
    public boolean testsVars(BDD var) {
      return false;
    }

    @Override
    public BDD project(BDD var) {
      return null;
    }

    @Override
    public BDD forAll(BDD var) {
      return null;
    }

    @Override
    public BDD unique(BDD var) {
      return null;
    }

    @Override
    public BDD restrict(BDD var) {
      return null;
    }

    @Override
    public BDD restrictWith(BDD var) {
      return null;
    }

    @Override
    public BDD simplify(BDD d) {
      return null;
    }

    @Override
    public BDD support() {
      return null;
    }

    @Override
    BDD apply(BDD that, BDDOp opr, boolean makeNew) {
      return null;
    }

    @Override
    public BDD applyWith(BDD that, BDDOp opr) {
      return null;
    }

    @Override
    public BDD applyAll(BDD that, BDDOp opr, BDD var) {
      return null;
    }

    @Override
    public BDD applyEx(BDD that, BDDOp opr, BDD var) {
      return null;
    }

    @Override
    public BDD transform(BDD rel, BDDPairing pair) {
      return null;
    }

    @Override
    public BDD applyUni(BDD that, BDDOp opr, BDD var) {
      return null;
    }

    @Override
    public BDD satOne() {
      return null;
    }

    @Override
    public BDD fullSatOne() {
      return null;
    }

    @Override
    public BitSet minAssignmentBits() {
      return null;
    }

    @Override
    public BDD randomFullSatOne(int seed) {
      return null;
    }

    @Override
    public BDD satOne(BDD var, boolean pol) {
      return null;
    }

    @Override
    public BDD replace(BDDPairing pair) {
      return null;
    }

    @Override
    public BDD replaceWith(BDDPairing pair) {
      return null;
    }

    @Override
    public int nodeCount() {
      return 0;
    }

    @Override
    public double pathCount() {
      return 0;
    }

    @Override
    public double satCount() {
      return 0;
    }

    @Override
    public int[] varProfile() {
      return new int[0];
    }

    @Override
    public boolean equals(@Nullable Object o) {
      return false;
    }

    @Override
    public int hashCode() {
      return 0;
    }

    @Override
    public void free() {}
  }

  @Override
  public long numOutstandingBDDs() {
    return 0;
  }

  @Override
  public BDD zero() {
    return null;
  }

  @Override
  public BDD one() {
    return null;
  }

  @Override
  protected void initialize(int nodenum, int cachesize) {}

  @Override
  public boolean isInitialized() {
    return false;
  }

  @Override
  public double setMinFreeNodes(double x) {
    return 0;
  }

  @Override
  public double setIncreaseFactor(double x) {
    return 0;
  }

  @Override
  public int setCacheRatio(int x) {
    return 0;
  }

  @Override
  protected BDD andAll(Iterable<BDD> bdds, boolean free) {
    return null;
  }

  @Override
  protected BDD orAll(Iterable<BDD> bdds, boolean free) {
    return null;
  }

  @Override
  public int setNodeTableSize(int n) {
    return 0;
  }

  @Override
  public int setCacheSize(int n) {
    return 0;
  }

  @Override
  public int varNum() {
    return 0;
  }

  @Override
  public int setVarNum(int num) {
    return 0;
  }

  @Override
  public BDD ithVar(int var) {
    return null;
  }

  @Override
  public BDD nithVar(int var) {
    return null;
  }

  @Override
  public void printAll() {}

  @Override
  public void printTable(BDD b) {}

  @Override
  public int level2Var(int level) {
    return 0;
  }

  @Override
  public int var2Level(int var) {
    return 0;
  }

  @Override
  public void setVarOrder(int[] neworder) {}

  @Override
  public BDDPairing makePair() {
    return null;
  }

  @Override
  public int duplicateVar(int var) {
    return 0;
  }

  @Override
  public int nodeCount(Collection<BDD> r) {
    return 0;
  }

  @Override
  public int getNodeTableSize() {
    return 0;
  }

  @Override
  public int getNodeNum() {
    return 0;
  }

  @Override
  public int getCacheSize() {
    return 0;
  }

  @Override
  public void printStat() {}

  @Override
  protected BDDDomain createDomain(int a, BigInteger b) {
    return null;
  }

  @Override
  protected BDDBitVector createBitVector(int a) {
    return null;
  }
}
