package com.fasdeq.airtok;



final class CommentLimit {
 static Object withLimit(Object original,int limit)throws Exception{
  if(limit<1)throw new IllegalArgumentException("limit");
  Class<?>
 type=original.getClass();
  return type.getConstructor(boolean.class,int.class,int.class,int.class).newInstance(
   GhostReflect.field(type,"enable").getBoolean(original),
   GhostReflect.field(type,"foldLines").getInt(original),
   GhostReflect.field(type,"expandLinesStep").getInt(original),limit);
 }
}
