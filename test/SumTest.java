import com.java.advanced.SumUtils;

import java.math.BigInteger;

public class SumTest {
    public static void main(String[] args) {
        BigInteger start = BigInteger.ZERO;
        BigInteger finish = BigInteger.valueOf(1_000_000_000L);
        BigInteger actual = SumUtils.sum(start, finish);
        BigInteger expected = BigInteger.valueOf(500_000_000_500_000_000L);
        System.out.printf("Expected\t: %s%n", expected);
        System.out.printf("Actual\t\t: %s%n", actual);
        assert expected.compareTo(actual) == 0;
    }
}
