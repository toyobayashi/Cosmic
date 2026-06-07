package api.model.dto;

public class UpdateAccountByGmDTO {
    private String name;
    private String password;
    private String email;
    private Integer gender;
    private Integer banned;
    private String banreason;
    private Integer webadmin;
    private Integer nxCredit;
    private Integer maplePoint;
    private Integer nxPrepaid;
    private Integer characterslots;
    private Integer gm;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Integer getGender() { return gender; }
    public void setGender(Integer gender) { this.gender = gender; }
    public Integer getBanned() { return banned; }
    public void setBanned(Integer banned) { this.banned = banned; }
    public String getBanreason() { return banreason; }
    public void setBanreason(String banreason) { this.banreason = banreason; }
    public Integer getWebadmin() { return webadmin; }
    public void setWebadmin(Integer webadmin) { this.webadmin = webadmin; }
    public Integer getNxCredit() { return nxCredit; }
    public void setNxCredit(Integer nxCredit) { this.nxCredit = nxCredit; }
    public Integer getMaplePoint() { return maplePoint; }
    public void setMaplePoint(Integer maplePoint) { this.maplePoint = maplePoint; }
    public Integer getNxPrepaid() { return nxPrepaid; }
    public void setNxPrepaid(Integer nxPrepaid) { this.nxPrepaid = nxPrepaid; }
    public Integer getCharacterslots() { return characterslots; }
    public void setCharacterslots(Integer characterslots) { this.characterslots = characterslots; }
    public Integer getGm() { return gm; }
    public void setGm(Integer gm) { this.gm = gm; }

    private String pin;
    private String pic;
    private String nick;
    private Integer mute;
    private Integer language;
    private Integer rewardpoints;
    private Integer votepoints;
    private String birthday;

    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }
    public String getPic() { return pic; }
    public void setPic(String pic) { this.pic = pic; }
    public String getNick() { return nick; }
    public void setNick(String nick) { this.nick = nick; }
    public Integer getMute() { return mute; }
    public void setMute(Integer mute) { this.mute = mute; }
    public Integer getLanguage() { return language; }
    public void setLanguage(Integer language) { this.language = language; }
    public Integer getRewardpoints() { return rewardpoints; }
    public void setRewardpoints(Integer rewardpoints) { this.rewardpoints = rewardpoints; }
    public Integer getVotepoints() { return votepoints; }
    public void setVotepoints(Integer votepoints) { this.votepoints = votepoints; }
    public String getBirthday() { return birthday; }
    public void setBirthday(String birthday) { this.birthday = birthday; }
}
