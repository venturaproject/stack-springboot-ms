import { z } from "zod"

const _emptyStringToUndefined = z.literal("").transform(() => undefined)

export const asOptional = <T>(schema: z.ZodType<T>) =>
  schema.optional().or(_emptyStringToUndefined)

export { z }
