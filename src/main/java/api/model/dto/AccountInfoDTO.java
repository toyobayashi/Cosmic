package api.model.dto;

public class AccountInfoDTO {
    private int id;
    private String name;
    private String email;
    private int gender;
    private int loggedin;
    private String lastlogin;
    private String createdat;
    private String birthday;
    private int banned;
    private String banreason;
    private int characterslots;
    private int webadmin;
    private String nick;
    private int mute;
    private Integer nxCredit;
    private Integer maplePoint;
    private Integer nxPrepaid;
    private int rewardpoints;
    private int votepoints;
    private int language;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public int getGender() { return gender; }
    public void setGender(int gender) { this.gender = gender; }
    public int getLoggedin() { return loggedin; }
    public void setLoggedin(int loggedin) { this.loggedin = loggedin; }
    public String getLastlogin() { return lastlogin; }
    public void setLastlogin(String lastlogin) { this.lastlogin = lastlogin; }
    public String getCreatedat() { return createdat; }
    public void setCreatedat(String createdat) { this.createdat = createdat; }
    public String getBirthday() { return birthday; }
    public void setBirthday(String birthday) { this.birthday = birthday; }
    public int getBanned() { return banned; }
    public void setBanned(int banned) { this.banned = banned; }
    public String getBanreason() { return banreason; }
    public void setBanreason(String banreason) { this.banreason = banreason; }
    public int getCharacterslots() { return characterslots; }
    public void setCharacterslots(int characterslots) { this.characterslots = characterslots; }
    public int getWebadmin() { return webadmin; }
    public void setWebadmin(int webadmin) { this.webadmin = webadmin; }
    public String getNick() { return nick; }
    public void setNick(String nick) { this.nick = nick; }
    public int getMute() { return mute; }
    public void setMute(int mute) { this.mute = mute; }
    public Integer getNxCredit() { return nxCredit; }
    public void setNxCredit(Integer nxCredit) { this.nxCredit = nxCredit; }
    public Integer getMaplePoint() { return maplePoint; }
    public void setMaplePoint(Integer maplePoint) { this.maplePoint = maplePoint; }
    public Integer getNxPrepaid() { return nxPrepaid; }
    public void setNxPrepaid(Integer nxPrepaid) { this.nxPrepaid = nxPrepaid; }
    public int getRewardpoints() { return rewardpoints; }
    public void setRewardpoints(int rewardpoints) { this.rewardpoints = rewardpoints; }
    public int getVotepoints() { return votepoints; }
    public void setVotepoints(int votepoints) { this.votepoints = votepoints; }
    public int getLanguage() { return language; }
    public void setLanguage(int language) { this.language = language; }
}
