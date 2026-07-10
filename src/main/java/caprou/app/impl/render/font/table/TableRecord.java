package caprou.app.impl.render.font.table;

public record TableRecord(
                            String tag,
                            long checksum,
                            long offset,
                            long length
                        ) { }
