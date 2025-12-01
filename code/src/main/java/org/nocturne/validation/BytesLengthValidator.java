package org.nocturne.validation;

import org.apache.log4j.Logger;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Validates that length of given value bytes is in specified range.
 *
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 17.04.15
 */
public class BytesLengthValidator extends Validator {
    private static final Logger logger = Logger.getLogger(BytesLengthValidator.class);

    private final int minimalLength;
    private final int maximalLength;
    private final Charset charset;

    /**
     * @param minimalLength Minimal bytes length.
     */
    public BytesLengthValidator(int minimalLength) {
        this(minimalLength, Integer.MAX_VALUE, StandardCharsets.UTF_8);
    }

    /**
     * @param minimalLength Minimal bytes length.
     * @param charset       Charset to get bytes.
     */
    public BytesLengthValidator(int minimalLength, Charset charset) {
        this(minimalLength, Integer.MAX_VALUE, charset);
    }

    /**
     * @param minimalLength Minimal bytes length.
     * @param maximalLength Maximal bytes length.
     */
    public BytesLengthValidator(int minimalLength, int maximalLength) {
        this(minimalLength, maximalLength, StandardCharsets.UTF_8);
    }

    /**
     * @param minimalLength Minimal bytes length.
     * @param maximalLength Maximal bytes length.
     * @param charset       Charset to get bytes.
     */
    public BytesLengthValidator(int minimalLength, int maximalLength, Charset charset) {
        this.minimalLength = minimalLength;
        this.maximalLength = maximalLength;
        this.charset = charset;
    }

    /**
     * @param value Value to be analyzed.
     * @throws ValidationException On validation error. It is good idea to pass
     *                             localized via captions value inside ValidationException,
     *                             like {@code return new ValidationException($("Field can't be empty"));}.
     */
    @Override
    public void run(String value) throws ValidationException {
        if (value == null) {
            if (minimalLength > 0) {
                throw new ValidationException($(
                        "Field should contain at least {0,number,#} bytes", minimalLength
                ));
            } else {
                logger.error("Value is `null` but minimalLength <= 0.");
                throw new ValidationException($("Field should not be empty"));
            }
        }

        int length = value.getBytes(charset).length;

        if (length < minimalLength) {
            throw new ValidationException($(
                    "Field should contain at least {0,number,#} bytes", minimalLength
            ));
        }

        if (length > maximalLength) {
            throw new ValidationException($(
                    "Field should contain no more than {0,number,#} bytes", maximalLength
            ));
        }
    }

    @Override
    public String toString() {
        return String.format(
                "BytesLengthValidator {minimalLength=%d, maximalLength=%d}", minimalLength, maximalLength
        );
    }
}
