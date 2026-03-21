package com.openclaw.digitalbeings.interfaces.cli;

import java.util.Locale;
import picocli.CommandLine;

public final class CliOutputFormatConverter implements CommandLine.ITypeConverter<CliOutputFormat> {

    @Override
    public CliOutputFormat convert(String value) {
        if (value == null) {
            return CliOutputFormat.TABLE;
        }
        try {
            return CliOutputFormat.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new CommandLine.TypeConversionException("Supported output formats: table, json");
        }
    }
}
