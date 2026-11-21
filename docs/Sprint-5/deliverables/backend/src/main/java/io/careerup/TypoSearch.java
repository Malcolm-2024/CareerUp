package io.careerup;
import java.util.*;
import java.text.Normalizer;
/** Small typo fallback. Normal SQL matches always take precedence. */
final class TypoSearch {
  private static String normalize(String s) {
    return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT);
  }
  static boolean eligible(String q) {
    return q != null && q.length() <= 150 && normalize(q).matches("[a-z0-9 ]{4,}");
  }
  static boolean matches(String q, Map<String,Object> row) {
    StringBuilder text=new StringBuilder();
    for(String key:List.of("name","email","title","description","bio","major","skills"))
      if(row.get(key)!=null) text.append(' ').append(row.get(key));
    String normalized=normalize(text.toString());
    String[] words=normalized.split("[^a-z0-9]+");
    for(String token:normalize(q).strip().split(" +")) {
      if(normalized.contains(token)) continue;
      if(token.length()<4) return false;
      int limit=token.length()>=8?2:1;
      boolean found=false;
      for(String word:words) if(Math.abs(word.length()-token.length())<=limit && distance(token,word)<=limit) {found=true;break;}
      if(!found)return false;
    }
    return true;
  }
  static int distance(String a,String b) {
    int[][] d=new int[a.length()+1][b.length()+1];
    for(int i=0;i<=a.length();i++)d[i][0]=i;
    for(int j=0;j<=b.length();j++)d[0][j]=j;
    for(int i=1;i<=a.length();i++)for(int j=1;j<=b.length();j++) {
      d[i][j]=Math.min(Math.min(d[i-1][j]+1,d[i][j-1]+1),d[i-1][j-1]+(a.charAt(i-1)==b.charAt(j-1)?0:1));
      if(i>1 && j>1 && a.charAt(i-1)==b.charAt(j-2) && a.charAt(i-2)==b.charAt(j-1))d[i][j]=Math.min(d[i][j],d[i-2][j-2]+1);
    }
    return d[a.length()][b.length()];
  }
}
